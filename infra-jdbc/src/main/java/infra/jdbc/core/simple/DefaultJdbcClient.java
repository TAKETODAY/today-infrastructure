/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.core.simple;

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.beans.BeanUtils;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.jdbc.core.BatchPreparedStatementSetter;
import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.PreparedStatementCreator;
import infra.jdbc.core.PreparedStatementCreatorFactory;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.core.RowCallbackHandler;
import infra.jdbc.core.RowMapper;
import infra.jdbc.core.SimplePropertyRowMapper;
import infra.jdbc.core.SingleColumnRowMapper;
import infra.jdbc.core.SqlParameterValue;
import infra.jdbc.core.namedparam.MapSqlParameterSource;
import infra.jdbc.core.namedparam.NamedParameterJdbcOperations;
import infra.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import infra.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import infra.jdbc.core.namedparam.SqlParameterSource;
import infra.jdbc.support.JdbcAccessor;
import infra.jdbc.support.KeyHolder;
import infra.jdbc.support.rowset.SqlRowSet;
import infra.util.Assert;

/**
 * The default implementation of {@link JdbcClient},
 * as created by the static factory methods.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Jiri Krokviak
 * @author Yanming Zhou
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcClient#create(DataSource)
 * @see JdbcClient#create(JdbcOperations)
 * @see JdbcClient#create(NamedParameterJdbcOperations)
 * @since 4.0
 */
final class DefaultJdbcClient implements JdbcClient {

  private final NamedParameterJdbcOperations namedParamOps;

  private final ConversionService conversionService;

  private final Map<Class<?>, RowMapper<?>> rowMapperCache = new ConcurrentHashMap<>();

  public DefaultJdbcClient(DataSource dataSource) {
    this(new JdbcTemplate(dataSource));
  }

  public DefaultJdbcClient(JdbcOperations jdbcTemplate) {
    this(new NamedParameterJdbcTemplate(jdbcTemplate), null);
  }

  public DefaultJdbcClient(NamedParameterJdbcOperations jdbcTemplate, @Nullable ConversionService conversionService) {
    Assert.notNull(jdbcTemplate, "JdbcTemplate is required");
    this.namedParamOps = jdbcTemplate;
    this.conversionService =
            (conversionService != null ? conversionService : DefaultConversionService.getSharedInstance());
  }

  @Override
  public StatementSpec sql(String sql) {
    return new DefaultStatementSpec(sql, this.namedParamOps);
  }

  private final class DefaultStatementSpec implements StatementSpec {

    private final String sql;

    private JdbcOperations classicOps;

    private NamedParameterJdbcOperations namedParamOps;

    @Nullable
    private JdbcTemplate customTemplate;

    private final List<@Nullable Object> indexedParams = new ArrayList<>();

    private final MapSqlParameterSource namedParams = new MapSqlParameterSource();

    private SqlParameterSource namedParamSource = this.namedParams;

    public DefaultStatementSpec(String sql, NamedParameterJdbcOperations namedParamOps) {
      this.sql = sql;
      this.classicOps = namedParamOps.getJdbcOperations();
      this.namedParamOps = namedParamOps;
    }

    private JdbcTemplate enforceCustomTemplate() {
      if (this.customTemplate == null) {
        if (!(this.classicOps instanceof JdbcAccessor original)) {
          throw new IllegalStateException(
                  "Needs to be bound to a JdbcAccessor for custom settings support: " + this.classicOps);
        }
        this.customTemplate = new JdbcTemplate(original);
        this.classicOps = this.customTemplate;
        this.namedParamOps = (this.namedParamOps instanceof NamedParameterJdbcTemplate originalNamedParam ?
                new NamedParameterJdbcTemplate(originalNamedParam, this.customTemplate) :
                new NamedParameterJdbcTemplate(this.customTemplate));
      }
      return this.customTemplate;
    }

    @Override
    public StatementSpec withFetchSize(int fetchSize) {
      enforceCustomTemplate().setFetchSize(fetchSize);
      return this;
    }

    @Override
    public StatementSpec withMaxRows(int maxRows) {
      enforceCustomTemplate().setMaxRows(maxRows);
      return this;
    }

    @Override
    public StatementSpec withQueryTimeout(int queryTimeout) {
      enforceCustomTemplate().setQueryTimeout(queryTimeout);
      return this;
    }

    @Override
    public StatementSpec param(@Nullable Object value) {
      validateIndexedParamValue(value);
      this.indexedParams.add(value);
      return this;
    }

    @Override
    public StatementSpec param(int jdbcIndex, @Nullable Object value) {
      addIndexedParam(this.indexedParams, jdbcIndex, value);
      return this;
    }

    @Override
    public StatementSpec param(int jdbcIndex, @Nullable Object value, int sqlType) {
      return param(jdbcIndex, new SqlParameterValue(sqlType, value));
    }

    @Override
    public StatementSpec param(String name, @Nullable Object value) {
      this.namedParams.addValue(name, value);
      return this;
    }

    @Override
    public StatementSpec param(String name, @Nullable Object value, int sqlType) {
      this.namedParams.addValue(name, value, sqlType);
      return this;
    }

    @Override
    public StatementSpec params(Object... values) {
      Collections.addAll(this.indexedParams, values);
      return this;
    }

    @Override
    public StatementSpec params(List<?> values) {
      this.indexedParams.addAll(values);
      return this;
    }

    @Override
    public StatementSpec params(Map<String, ?> paramMap) {
      this.namedParams.addValues(paramMap);
      return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public StatementSpec paramSource(Object namedParamObject) {
      this.namedParamSource = (namedParamObject instanceof Map map ?
              new MapSqlParameterSource(map) :
              new SimplePropertySqlParameterSource(namedParamObject));
      return this;
    }

    @Override
    public StatementSpec paramSource(SqlParameterSource namedParamSource) {
      this.namedParamSource = namedParamSource;
      return this;
    }

    @Override
    public ResultQuerySpec query() {
      return (useNamedParams() ?
              new NamedParamResultQuerySpec() :
              new IndexedParamResultQuerySpec());
    }

    @Override
    @SuppressWarnings({ "unchecked", "NullAway" })
    public <T> MappedQuerySpec<T> query(Class<T> mappedClass) {
      RowMapper<?> rowMapper = rowMapperCache.computeIfAbsent(mappedClass, key ->
              BeanUtils.isSimpleProperty(mappedClass) ?
                      new SingleColumnRowMapper<>(mappedClass, conversionService) :
                      new SimplePropertyRowMapper<>(mappedClass, conversionService));
      return query((RowMapper<T>) rowMapper);
    }

    @Override
    public <T> MappedQuerySpec<T> query(RowMapper<T> rowMapper) {
      return (useNamedParams() ?
              new NamedParamMappedQuerySpec<>(rowMapper) :
              new IndexedParamMappedQuerySpec<>(rowMapper));
    }

    @Override
    public void query(RowCallbackHandler rch) {
      if (useNamedParams()) {
        this.namedParamOps.query(this.sql, this.namedParamSource, rch);
      }
      else {
        this.classicOps.query(statementCreatorForIndexedParams(), rch);
      }
    }

    @Override
    public <T> T query(ResultSetExtractor<T> rse) {
      T result = (useNamedParams() ?
              this.namedParamOps.query(this.sql, this.namedParamSource, rse) :
              this.classicOps.query(statementCreatorForIndexedParams(), rse));
      Assert.state(result != null, "No result from ResultSetExtractor");
      return result;
    }

    @Override
    public int update() {
      return (useNamedParams() ?
              this.namedParamOps.update(this.sql, this.namedParamSource) :
              this.classicOps.update(statementCreatorForIndexedParams()));
    }

    @Override
    public int update(KeyHolder generatedKeyHolder) {
      return (useNamedParams() ?
              this.namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder) :
              this.classicOps.update(statementCreatorForIndexedParamsWithKeys(null), generatedKeyHolder));
    }

    @Override
    public int update(KeyHolder generatedKeyHolder, String... keyColumnNames) {
      return (useNamedParams() ?
              this.namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder, keyColumnNames) :
              this.classicOps.update(statementCreatorForIndexedParamsWithKeys(keyColumnNames), generatedKeyHolder));
    }

    @Override
    public BatchSpec batch() {
      return new DefaultBatchSpec();
    }

    private boolean useNamedParams() {
      boolean hasNamedParams = (this.namedParams.hasValues() || this.namedParamSource != this.namedParams);
      if (hasNamedParams && !this.indexedParams.isEmpty()) {
        throw new IllegalStateException("Configure either named or indexed parameters, not both");
      }
      if (this.namedParams.hasValues() && this.namedParamSource != this.namedParams) {
        throw new IllegalStateException(
                "Configure either individual named parameters or a SqlParameterSource, not both");
      }
      return hasNamedParams;
    }

    private PreparedStatementCreator statementCreatorForIndexedParams() {
      return new PreparedStatementCreatorFactory(this.sql).newPreparedStatementCreator(this.indexedParams);
    }

    private PreparedStatementCreator statementCreatorForIndexedParamsWithKeys(String @Nullable [] keyColumnNames) {
      PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(this.sql);
      if (keyColumnNames != null) {
        pscf.setGeneratedKeysColumnNames(keyColumnNames);
      }
      else {
        pscf.setReturnGeneratedKeys(true);
      }
      return pscf.newPreparedStatementCreator(this.indexedParams);
    }

    private static void addIndexedParam(List<@Nullable Object> indexedParams, int jdbcIndex, @Nullable Object value) {
      if (jdbcIndex < 1) {
        throw new IllegalArgumentException("Invalid JDBC index: needs to start at 1");
      }
      validateIndexedParamValue(value);
      int index = jdbcIndex - 1;
      int size = indexedParams.size();
      if (index < size) {
        indexedParams.set(index, value);
      }
      else {
        for (int i = size; i < index; i++) {
          indexedParams.add(null);
        }
        indexedParams.add(value);
      }
    }

    private static void validateIndexedParamValue(@Nullable Object value) {
      if (value instanceof Iterable) {
        throw new IllegalArgumentException("Invalid positional parameter value of type Iterable (" +
                value.getClass().getSimpleName() +
                "): Parameter expansion is only supported with named parameters.");
      }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static SqlParameterSource toSqlParameterSource(Object namedParamObject) {
      if (namedParamObject instanceof SqlParameterSource sqlParameterSource) {
        return sqlParameterSource;
      }
      return (namedParamObject instanceof Map map ?
              new MapSqlParameterSource(map) :
              new SimplePropertySqlParameterSource(namedParamObject));
    }

    private final class DefaultBatchSpec implements BatchSpec {

      private final List<Object[]> indexedBatch = new ArrayList<>();

      private final List<SqlParameterSource> namedBatch = new ArrayList<>();

      private @Nullable Boolean usingNamedParams;

      @Override
      public BatchSpec entry(Consumer<BatchEntry> entryConsumer) {
        DefaultBatchEntry entry = new DefaultBatchEntry();
        entryConsumer.accept(entry);
        addEntry(entry);
        return this;
      }

      @Override
      public BatchSpec entry(List<?> values) {
        DefaultBatchEntry entry = new DefaultBatchEntry();
        entry.indexedParams.addAll(values);
        addEntry(entry);
        return this;
      }

      @Override
      public BatchSpec entry(Map<String, ?> paramMap) {
        DefaultBatchEntry entry = new DefaultBatchEntry();
        entry.namedParams.addValues(paramMap);
        addEntry(entry);
        return this;
      }

      @Override
      public BatchSpec entries(Object... namedParamObjects) {
        return entries(Arrays.asList(namedParamObjects));
      }

      @Override
      public BatchSpec entries(List<?> namedParamObjects) {
        for (Object namedParamObject : namedParamObjects) {
          addNamedEntry(toSqlParameterSource(namedParamObject));
        }
        return this;
      }

      @Override
      public int[] update() {
        return (Boolean.TRUE.equals(this.usingNamedParams) ?
                namedParamOps.batchUpdate(sql, this.namedBatch.toArray(new SqlParameterSource[0])) :
                classicOps.batchUpdate(sql, this.indexedBatch));
      }

      @Override
      public int[] update(KeyHolder generatedKeyHolder) {
        return doUpdate(generatedKeyHolder, null);
      }

      @Override
      public int[] update(KeyHolder generatedKeyHolder, String... keyColumnNames) {
        return doUpdate(generatedKeyHolder, keyColumnNames);
      }

      private int[] doUpdate(KeyHolder generatedKeyHolder, String @Nullable [] keyColumnNames) {
        if (Boolean.TRUE.equals(this.usingNamedParams)) {
          if (keyColumnNames != null) {
            return namedParamOps.batchUpdate(sql, this.namedBatch.toArray(new SqlParameterSource[0]),
                    generatedKeyHolder, keyColumnNames);
          }
          else {
            return namedParamOps.batchUpdate(sql, this.namedBatch.toArray(new SqlParameterSource[0]),
                    generatedKeyHolder);
          }
        }
        else {
          if (this.indexedBatch.isEmpty()) {
            return new int[0];
          }
          PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sql);
          if (keyColumnNames != null) {
            pscf.setGeneratedKeysColumnNames(keyColumnNames);
          }
          else {
            pscf.setReturnGeneratedKeys(true);
          }
          PreparedStatementCreator psc = pscf.newPreparedStatementCreator(this.indexedBatch.get(0));
          return classicOps.batchUpdate(psc, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
              if (i == 0) {
                // indexedBatch[0] is already set by pscf.newPreparedStatementCreator()
                return;
              }
              pscf.newPreparedStatementSetter(indexedBatch.get(i)).setValues(ps);
            }

            @Override
            public int getBatchSize() {
              return indexedBatch.size();
            }
          }, generatedKeyHolder);
        }
      }

      private void addEntry(DefaultBatchEntry entry) {
        boolean hasIndexed = !entry.indexedParams.isEmpty();
        boolean hasNamed = (entry.namedParams.hasValues() || entry.namedParamSource != entry.namedParams);
        if (hasIndexed && hasNamed) {
          throw new IllegalStateException("Configure either named or indexed parameters, not both");
        }
        if (entry.namedParams.hasValues() && entry.namedParamSource != entry.namedParams) {
          throw new IllegalStateException(
                  "Configure either individual named parameters or a SqlParameterSource, not both");
        }
        if (hasNamed) {
          addNamedEntry(entry.namedParamSource);
        }
        else if (hasIndexed) {
          addIndexedEntry(entry.indexedParams.toArray());
        }
        else {
          throw new IllegalStateException("Configure at least one parameter for each batch entry");
        }
      }

      private void addNamedEntry(SqlParameterSource namedParamSource) {
        enforceParamStyle(true);
        this.namedBatch.add(namedParamSource);
      }

      private void addIndexedEntry(@Nullable Object[] indexedParams) {
        enforceParamStyle(false);
        this.indexedBatch.add(indexedParams);
      }

      private void enforceParamStyle(boolean named) {
        if (this.usingNamedParams == null) {
          this.usingNamedParams = named;
        }
        else if (this.usingNamedParams != named) {
          throw new IllegalStateException(
                  "Configure either named or indexed parameters for all batch entries, not both");
        }
      }
    }


    private static class DefaultBatchEntry implements BatchEntry {

      private final List<@Nullable Object> indexedParams = new ArrayList<>();

      private final MapSqlParameterSource namedParams = new MapSqlParameterSource();

      private SqlParameterSource namedParamSource = this.namedParams;

      @Override
      public BatchEntry param(@Nullable Object value) {
        validateIndexedParamValue(value);
        this.indexedParams.add(value);
        return this;
      }

      @Override
      public BatchEntry param(int jdbcIndex, @Nullable Object value) {
        addIndexedParam(this.indexedParams, jdbcIndex, value);
        return this;
      }

      @Override
      public BatchEntry param(int jdbcIndex, @Nullable Object value, int sqlType) {
        return param(jdbcIndex, new SqlParameterValue(sqlType, value));
      }

      @Override
      public BatchEntry param(String name, @Nullable Object value) {
        this.namedParams.addValue(name, value);
        return this;
      }

      @Override
      public BatchEntry param(String name, @Nullable Object value, int sqlType) {
        this.namedParams.addValue(name, value, sqlType);
        return this;
      }

      @Override
      public BatchEntry paramSource(Object namedParamObject) {
        this.namedParamSource = toSqlParameterSource(namedParamObject);
        return this;
      }

      @Override
      public BatchEntry paramSource(SqlParameterSource namedParamSource) {
        this.namedParamSource = namedParamSource;
        return this;
      }
    }

    private final class IndexedParamResultQuerySpec implements ResultQuerySpec {

      @Override
      public SqlRowSet rowSet() {
        return classicOps.queryForRowSet(sql, indexedParams.toArray());
      }

      @Override
      public List<Map<String, Object>> listOfRows() {
        return classicOps.queryForList(sql, indexedParams.toArray());
      }

      @Override
      public Map<String, Object> singleRow() {
        return classicOps.queryForMap(sql, indexedParams.toArray());
      }

      @Override
      public List<Object> singleColumn() {
        return classicOps.queryForList(sql, Object.class, indexedParams.toArray());
      }
    }

    private final class NamedParamResultQuerySpec implements ResultQuerySpec {

      @Override
      public SqlRowSet rowSet() {
        return namedParamOps.queryForRowSet(sql, namedParamSource);
      }

      @Override
      public List<Map<String, Object>> listOfRows() {
        return namedParamOps.queryForList(sql, namedParamSource);
      }

      @Override
      public Map<String, Object> singleRow() {
        return namedParamOps.queryForMap(sql, namedParamSource);
      }

      @Override
      public List<Object> singleColumn() {
        return namedParamOps.queryForList(sql, namedParamSource, Object.class);
      }
    }

    private class IndexedParamMappedQuerySpec<T> implements MappedQuerySpec<T> {

      private final RowMapper<T> rowMapper;

      public IndexedParamMappedQuerySpec(RowMapper<T> rowMapper) {
        this.rowMapper = rowMapper;
      }

      @Override
      public Stream<T> stream() {
        return classicOps.queryForStream(sql, this.rowMapper, indexedParams.toArray());
      }

      @Override
      public List<T> list() {
        return classicOps.query(sql, this.rowMapper, indexedParams.toArray());
      }
    }

    private class NamedParamMappedQuerySpec<T> implements MappedQuerySpec<T> {

      private final RowMapper<T> rowMapper;

      public NamedParamMappedQuerySpec(RowMapper<T> rowMapper) {
        this.rowMapper = rowMapper;
      }

      @Override
      public Stream<T> stream() {
        return namedParamOps.queryForStream(sql, namedParamSource, this.rowMapper);
      }

      @Override
      public List<T> list() {
        return namedParamOps.query(sql, namedParamSource, this.rowMapper);
      }
    }
  }

}
