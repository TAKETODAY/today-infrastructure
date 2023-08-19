/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.core.simple;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import cn.taketoday.dao.support.DataAccessUtils;
import cn.taketoday.jdbc.core.JdbcOperations;
import cn.taketoday.jdbc.core.ResultSetExtractor;
import cn.taketoday.jdbc.core.RowCallbackHandler;
import cn.taketoday.jdbc.core.RowMapper;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcOperations;
import cn.taketoday.jdbc.core.namedparam.SqlParameterSource;
import cn.taketoday.jdbc.support.KeyHolder;
import cn.taketoday.jdbc.support.rowset.SqlRowSet;

/**
 * A fluent {@link JdbcClient} with common JDBC query and update operations,
 * supporting JDBC-style positional as well as Spring-style named parameters
 * with a convenient unified facade for JDBC PreparedStatement execution.
 *
 * <p>An example for retrieving a query result as a {@code java.util.Optional}:
 * <pre class="code">
 * Optional&lt;Integer&gt; value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
 *     .param("id", 3)
 *     .query((rs, rowNum) -> rs.getInt(1))
 *     .optional();
 * </pre>
 *
 * <p>Delegates to {@link cn.taketoday.jdbc.core.JdbcTemplate} and
 * {@link cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
 * For complex JDBC operations, e.g. batch inserts and stored procedure calls,
 * you may use those lower-level template classes directly - or alternatively,
 * {@link SimpleJdbcInsert} and {@link SimpleJdbcCall}.
 *
 * @author Juergen Hoeller
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see JdbcOperations
 * @see NamedParameterJdbcOperations
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @see cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate
 * @since 4.0
 */
public interface JdbcClient {

  /**
   * The starting point for any JDBC operation: a custom SQL String.
   *
   * @param sql the SQL query or update statement as a String
   * @return a chained statement specification
   */
  StatementSpec sql(String sql);

  // Static factory methods

  /**
   * Create a {@code JdbcClient} for the given {@link DataSource}.
   *
   * @param dataSource the DataSource to obtain connections from
   */
  static JdbcClient create(DataSource dataSource) {
    return new DefaultJdbcClient(dataSource);
  }

  /**
   * Create a {@code JdbcClient} for the given {@link JdbcOperations} delegate,
   * typically an {@link cn.taketoday.jdbc.core.JdbcTemplate}.
   * <p>Use this factory method to reuse existing {@code JdbcTemplate} configuration,
   * including its {@code DataSource}.
   *
   * @param jdbcTemplate the delegate to perform operations on
   */
  static JdbcClient create(JdbcOperations jdbcTemplate) {
    return new DefaultJdbcClient(jdbcTemplate);
  }

  /**
   * Create a {@code JdbcClient} for the given {@link NamedParameterJdbcOperations} delegate,
   * typically an {@link cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
   * <p>Use this factory method to reuse existing {@code NamedParameterJdbcTemplate}
   * configuration, including its underlying {@code JdbcTemplate} and {@code DataSource}.
   *
   * @param jdbcTemplate the delegate to perform operations on
   */
  static JdbcClient create(NamedParameterJdbcOperations jdbcTemplate) {
    return new DefaultJdbcClient(jdbcTemplate);
  }

  /**
   * A statement specification for parameter bindings and query/update execution.
   */
  interface StatementSpec {

    /**
     * Bind a positional JDBC statement parameter for "?" placeholder resolution
     * by implicit order of parameter value registration.
     * <p>This is primarily intended for statements with a single parameter
     * or very few parameters, registering each parameter value in the order
     * of the parameter's occurrence in the SQL statement.
     *
     * @param value the parameter value to bind
     * @return this statement specification (for chaining)
     * @see java.sql.PreparedStatement#setObject(int, Object)
     */
    StatementSpec param(Object value);

    /**
     * Bind a positional JDBC statement parameter for "?" placeholder resolution
     * by explicit JDBC statement parameter index.
     *
     * @param jdbcIndex the JDBC-style index (starting with 1)
     * @param value the parameter value to bind
     * @return this statement specification (for chaining)
     * @see java.sql.PreparedStatement#setObject(int, Object)
     */
    StatementSpec param(int jdbcIndex, Object value);

    /**
     * Bind a positional JDBC statement parameter for "?" placeholder resolution
     * by explicit JDBC statement parameter index.
     *
     * @param jdbcIndex the JDBC-style index (starting with 1)
     * @param value the parameter value to bind
     * @param sqlType the associated SQL type (see {@link java.sql.Types})
     * @return this statement specification (for chaining)
     * @see java.sql.PreparedStatement#setObject(int, Object, int)
     */
    StatementSpec param(int jdbcIndex, Object value, int sqlType);

    /**
     * Bind a named statement parameter for ":x" placeholder resolution,
     * with each "x" name matching a ":x" placeholder in the SQL statement.
     *
     * @param name the parameter name
     * @param value the parameter value to bind
     * @return this statement specification (for chaining)
     * @see cn.taketoday.jdbc.core.namedparam.MapSqlParameterSource#addValue(String, Object)
     */
    StatementSpec param(String name, Object value);

    /**
     * Bind a named statement parameter for ":x" placeholder resolution,
     * with each "x" name matching a ":x" placeholder in the SQL statement.
     *
     * @param name the parameter name
     * @param value the parameter value to bind
     * @param sqlType the associated SQL type (see {@link java.sql.Types})
     * @return this statement specification (for chaining)
     * @see cn.taketoday.jdbc.core.namedparam.MapSqlParameterSource#addValue(String, Object, int)
     */
    StatementSpec param(String name, Object value, int sqlType);

    /**
     * Bind a var-args list of positional parameters for "?" placeholder resolution.
     * <p>The given list will be added to existing positional parameters, if any.
     * Each element from the complete list will be bound as a JDBC positional
     * parameter with a corresponding JDBC index (i.e. list index + 1).
     *
     * @param values the parameter values to bind
     * @return this statement specification (for chaining)
     * @see #param(Object)
     * @see #params(List)
     */
    StatementSpec params(Object... values);

    /**
     * Bind a list of positional parameters for "?" placeholder resolution.
     * <p>The given list will be added to existing positional parameters, if any.
     * Each element from the complete list will be bound as a JDBC positional
     * parameter with a corresponding JDBC index (i.e. list index + 1).
     *
     * @param values the parameter values to bind
     * @return this statement specification (for chaining)
     * @see #param(Object)
     */
    StatementSpec params(List<?> values);

    /**
     * Bind named statement parameters for ":x" placeholder resolution.
     * <p>The given map will be merged into existing named parameters, if any.
     *
     * @param paramMap a map of names and parameter values to bind
     * @return this statement specification (for chaining)
     * @see #param(String, Object)
     */
    StatementSpec params(Map<String, ?> paramMap);

    /**
     * Bind named statement parameters for ":x" placeholder resolution.
     * <p>The given parameter object will define all named parameters
     * based on its JavaBean properties, record components, or raw fields.
     * A Map instance can be provided as a complete parameter source as well.
     *
     * @param namedParamObject a custom parameter object (e.g. a JavaBean,
     * record class, or field holder) with named properties serving as
     * statement parameters
     * @return this statement specification (for chaining)
     * @see #paramSource(SqlParameterSource)
     * @see cn.taketoday.jdbc.core.namedparam.MapSqlParameterSource
     * @see cn.taketoday.jdbc.core.namedparam.SimplePropertySqlParameterSource
     */
    StatementSpec paramSource(Object namedParamObject);

    /**
     * Bind named statement parameters for ":x" placeholder resolution.
     * <p>The given parameter source will define all named parameters,
     * possibly associating specific SQL types with each value.
     *
     * @param namedParamSource a custom {@link SqlParameterSource} instance
     * @return this statement specification (for chaining)
     * @see cn.taketoday.jdbc.core.namedparam.AbstractSqlParameterSource#registerSqlType
     */
    StatementSpec paramSource(SqlParameterSource namedParamSource);

    /**
     * Proceed towards execution of a query, with several result options
     * available in the returned query specification.
     *
     * @return the result query specification
     * @see java.sql.PreparedStatement#executeQuery()
     */
    ResultQuerySpec query();

    /**
     * Proceed towards execution of a mapped query, with several options
     * available in the returned query specification.
     *
     * @param mappedClass the target class to apply a RowMapper for
     * (either a simple value type for a single column mapping or a
     * JavaBean / record class / field holder for a multi-column mapping)
     * @return the mapped query specification
     * @see #query(RowMapper)
     * @see cn.taketoday.jdbc.core.SingleColumnRowMapper
     * @see cn.taketoday.jdbc.core.SimplePropertyRowMapper
     */
    <T> MappedQuerySpec<T> query(Class<T> mappedClass);

    /**
     * Proceed towards execution of a mapped query, with several options
     * available in the returned query specification.
     *
     * @param rowMapper the callback for mapping each row in the ResultSet
     * @return the mapped query specification
     * @see java.sql.PreparedStatement#executeQuery()
     */
    <T> MappedQuerySpec<T> query(RowMapper<T> rowMapper);

    /**
     * Execute a query with the provided SQL statement,
     * processing each row with the given callback.
     *
     * @param rch a callback for processing each row in the ResultSet
     * @see java.sql.PreparedStatement#executeQuery()
     */
    void query(RowCallbackHandler rch);

    /**
     * Execute a query with the provided SQL statement,
     * returning a result object for the entire ResultSet.
     *
     * @param rse a callback for processing the entire ResultSet
     * @return the value returned by the ResultSetExtractor
     * @see java.sql.PreparedStatement#executeQuery()
     */
    <T> T query(ResultSetExtractor<T> rse);

    /**
     * Execute the provided SQL statement as an update.
     *
     * @return the number of rows affected
     * @see java.sql.PreparedStatement#executeUpdate()
     */
    int update();

    /**
     * Execute the provided SQL statement as an update.
     *
     * @param generatedKeyHolder a KeyHolder that will hold the generated keys
     * (typically a {@link cn.taketoday.jdbc.support.GeneratedKeyHolder})
     * @return the number of rows affected
     * @see java.sql.PreparedStatement#executeUpdate()
     */
    int update(KeyHolder generatedKeyHolder);
  }

  /**
   * A specification for simple result queries.
   */
  interface ResultQuerySpec {

    /**
     * Retrieve the result as a row set.
     *
     * @return a detached row set representation
     * of the original database result
     */
    SqlRowSet rowSet();

    /**
     * Retrieve the result as a list of rows,
     * retaining the order from the original database result.
     *
     * @return a (potentially empty) list of rows,
     * with each result row represented as a map of
     * case-insensitive column names to column values
     */
    List<Map<String, Object>> listOfRows();

    /**
     * Retrieve a single row result.
     *
     * @return the result row represented as a map of
     * case-insensitive column names to column values
     */
    Map<String, Object> singleRow();

    /**
     * Retrieve a single column result,
     * retaining the order from the original database result.
     *
     * @return a (potentially empty) list of rows, with each
     * row represented as a column value of the given type
     */
    <T> List<T> singleColumn();

    /**
     * Retrieve a single value result.
     *
     * @return the single row represented as its single
     * column value of the given type
     * @see DataAccessUtils#requiredSingleResult(Collection)
     */
    default <T> T singleValue() {
      return DataAccessUtils.requiredSingleResult(singleColumn());
    }
  }

  /**
   * A specification for RowMapper-mapped queries.
   *
   * @param <T> the RowMapper-declared result type
   */
  interface MappedQuerySpec<T> {

    /**
     * Retrieve the result as a lazily resolved stream of mapped objects,
     * retaining the order from the original database result.
     *
     * @return the result Stream, containing mapped objects, needing to be
     * closed once fully processed (e.g. through a try-with-resources clause)
     */
    Stream<T> stream();

    /**
     * Retrieve the result as a pre-resolved list of mapped objects,
     * retaining the order from the original database result.
     *
     * @return the result as a detached List, containing mapped objects
     */
    List<T> list();

    /**
     * Retrieve the result as an order-preserving set of mapped objects.
     *
     * @return the result as a detached Set, containing mapped objects
     * @see #list()
     * @see LinkedHashSet
     */
    default Set<T> set() {
      return new LinkedHashSet<>(list());
    }

    /**
     * Retrieve a single result, if available, as an {@link Optional} handle.
     *
     * @return an Optional handle with a single result object or none
     * @see #list()
     * @see DataAccessUtils#optionalResult(Collection)
     */
    default Optional<T> optional() {
      return DataAccessUtils.optionalResult(list());
    }

    /**
     * Retrieve a single result as a required object instance.
     *
     * @return the single result object (never {@code null})
     * @see #list()
     * @see DataAccessUtils#requiredSingleResult(Collection)
     */
    default T single() {
      return DataAccessUtils.requiredSingleResult(list());
    }
  }

}
