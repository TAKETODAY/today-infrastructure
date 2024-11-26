/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.core.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.beans.BeanUtils;
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
import infra.jdbc.support.KeyHolder;
import infra.jdbc.support.rowset.SqlRowSet;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * The default implementation of {@link JdbcClient},
 * as created by the static factory methods.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcClient#create(DataSource)
 * @see JdbcClient#create(JdbcOperations)
 * @see JdbcClient#create(NamedParameterJdbcOperations)
 * @since 4.0
 */
final class DefaultJdbcClient implements JdbcClient {

  private final JdbcOperations classicOps;

  private final NamedParameterJdbcOperations namedParamOps;

  private final ConcurrentHashMap<Class<?>, RowMapper<?>> rowMapperCache = new ConcurrentHashMap<>();

  public DefaultJdbcClient(DataSource dataSource) {
    this.classicOps = new JdbcTemplate(dataSource);
    this.namedParamOps = new NamedParameterJdbcTemplate(this.classicOps);
  }

  public DefaultJdbcClient(JdbcOperations jdbcTemplate) {
    Assert.notNull(jdbcTemplate, "JdbcTemplate is required");
    this.classicOps = jdbcTemplate;
    this.namedParamOps = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  public DefaultJdbcClient(NamedParameterJdbcOperations jdbcTemplate) {
    Assert.notNull(jdbcTemplate, "JdbcTemplate is required");
    this.classicOps = jdbcTemplate.getJdbcOperations();
    this.namedParamOps = jdbcTemplate;
  }

  @Override
  public StatementSpec sql(String sql) {
    return new DefaultStatementSpec(sql);
  }

  private class DefaultStatementSpec implements StatementSpec {

    private final String sql;

    private final ArrayList<Object> indexedParams = new ArrayList<>();

    private final MapSqlParameterSource namedParams = new MapSqlParameterSource();

    private SqlParameterSource namedParamSource = this.namedParams;

    public DefaultStatementSpec(String sql) {
      this.sql = sql;
    }

    @Override
    public StatementSpec param(@Nullable Object value) {
      validateIndexedParamValue(value);
      this.indexedParams.add(value);
      return this;
    }

    @Override
    public StatementSpec param(int jdbcIndex, @Nullable Object value) {
      if (jdbcIndex < 1) {
        throw new IllegalArgumentException("Invalid JDBC index: needs to start at 1");
      }
      validateIndexedParamValue(value);
      int index = jdbcIndex - 1;
      int size = this.indexedParams.size();
      if (index < size) {
        this.indexedParams.set(index, value);
      }
      else {
        for (int i = size; i < index; i++) {
          this.indexedParams.add(null);
        }
        this.indexedParams.add(value);
      }
      return this;
    }

    @Override
    public StatementSpec param(int jdbcIndex, @Nullable Object value, int sqlType) {
      return param(jdbcIndex, new SqlParameterValue(sqlType, value));
    }

    private void validateIndexedParamValue(@Nullable Object value) {
      if (value instanceof Iterable) {
        throw new IllegalArgumentException("Invalid positional parameter value of type Iterable (" +
                value.getClass().getSimpleName() +
                "): Parameter expansion is only supported with named parameters.");
      }
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> MappedQuerySpec<T> query(Class<T> mappedClass) {
      RowMapper<?> rowMapper = rowMapperCache.computeIfAbsent(mappedClass, key ->
              BeanUtils.isSimpleProperty(mappedClass) ? new SingleColumnRowMapper<>(mappedClass) :
              new SimplePropertyRowMapper<>(mappedClass));
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
        namedParamOps.query(this.sql, this.namedParamSource, rch);
      }
      else {
        classicOps.query(statementCreatorForIndexedParams(), rch);
      }
    }

    @Override
    public <T> T query(ResultSetExtractor<T> rse) {
      T result = (useNamedParams() ?
                  namedParamOps.query(this.sql, this.namedParamSource, rse) :
                  classicOps.query(statementCreatorForIndexedParams(), rse));
      Assert.state(result != null, "No result from ResultSetExtractor");
      return result;
    }

    @Override
    public int update() {
      return (useNamedParams() ?
              namedParamOps.update(this.sql, this.namedParamSource) :
              classicOps.update(statementCreatorForIndexedParams()));
    }

    @Override
    public int update(KeyHolder generatedKeyHolder) {
      return (useNamedParams() ?
              namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder) :
              classicOps.update(statementCreatorForIndexedParamsWithKeys(null), generatedKeyHolder));
    }

    @Override
    public int update(KeyHolder generatedKeyHolder, String... keyColumnNames) {
      return (useNamedParams() ?
              namedParamOps.update(this.sql, this.namedParamSource, generatedKeyHolder, keyColumnNames) :
              classicOps.update(statementCreatorForIndexedParamsWithKeys(keyColumnNames), generatedKeyHolder));
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

    private PreparedStatementCreator statementCreatorForIndexedParamsWithKeys(@Nullable String[] keyColumnNames) {
      PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(this.sql);
      if (keyColumnNames != null) {
        pscf.setGeneratedKeysColumnNames(keyColumnNames);
      }
      else {
        pscf.setReturnGeneratedKeys(true);
      }
      return pscf.newPreparedStatementCreator(this.indexedParams);
    }

    private class IndexedParamResultQuerySpec implements ResultQuerySpec {

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

    private class NamedParamResultQuerySpec implements ResultQuerySpec {

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
