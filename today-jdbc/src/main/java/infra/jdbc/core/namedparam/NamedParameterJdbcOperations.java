/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.core.namedparam;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import infra.dao.DataAccessException;
import infra.jdbc.IncorrectResultSetColumnCountException;
import infra.jdbc.core.ColumnMapRowMapper;
import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.PreparedStatementCallback;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.core.RowCallbackHandler;
import infra.jdbc.core.RowMapper;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.SingleColumnRowMapper;
import infra.jdbc.core.SqlRowSetResultSetExtractor;
import infra.jdbc.support.KeyHolder;
import infra.jdbc.core.simple.JdbcClient;
import infra.jdbc.support.GeneratedKeyHolder;
import infra.jdbc.support.rowset.SqlRowSet;
import infra.lang.Nullable;

/**
 * Interface specifying a basic set of JDBC operations allowing the use
 * of named parameters rather than the traditional '?' placeholders.
 *
 * <p>This is an alternative to the classic
 * {@link JdbcOperations} interface,
 * implemented by {@link NamedParameterJdbcTemplate}. This interface is not
 * often used directly, but provides a useful option to enhance testability,
 * as it can easily be mocked or stubbed.
 *
 * <p>there is a unified JDBC access facade available in
 * the form of {@link JdbcClient}.
 * {@code JdbcClient} provides a fluent API style for common JDBC queries/updates
 * with flexible use of indexed or named parameters. It delegates to
 * {@code JdbcOperations}/{@code NamedParameterJdbcOperations} for actual execution.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NamedParameterJdbcTemplate
 * @see JdbcOperations
 * @since 4.0
 */
public interface NamedParameterJdbcOperations {

  /**
   * Expose the classic Framework JdbcTemplate to allow invocation of
   * classic JDBC operations.
   */
  JdbcOperations getJdbcOperations();

  /**
   * Execute a JDBC data access operation, implemented as callback action
   * working on a JDBC PreparedStatement. This allows for implementing arbitrary
   * data access operations on a single Statement, within Framework's managed
   * JDBC environment: that is, participating in Framework-managed transactions
   * and converting JDBC SQLExceptions into Framework's DataAccessException hierarchy.
   * <p>The callback action can return a result object, for example a
   * domain object or a collection of domain objects.
   *
   * @param sql the SQL to execute
   * @param paramSource container of arguments to bind to the query
   * @param action callback object that specifies the action
   * @return a result object returned by the action, or {@code null}
   * @throws DataAccessException if there is any problem
   */
  @Nullable
  <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
          throws DataAccessException;

  /**
   * Execute a JDBC data access operation, implemented as callback action
   * working on a JDBC PreparedStatement. This allows for implementing arbitrary
   * data access operations on a single Statement, within Framework's managed
   * JDBC environment: that is, participating in Framework-managed transactions
   * and converting JDBC SQLExceptions into Framework's DataAccessException hierarchy.
   * <p>The callback action can return a result object, for example a
   * domain object or a collection of domain objects.
   *
   * @param sql the SQL to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param action callback object that specifies the action
   * @return a result object returned by the action, or {@code null}
   * @throws DataAccessException if there is any problem
   */
  @Nullable
  <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
          throws DataAccessException;

  /**
   * Execute a JDBC data access operation, implemented as callback action
   * working on a JDBC PreparedStatement. This allows for implementing arbitrary
   * data access operations on a single Statement, within Framework's managed
   * JDBC environment: that is, participating in Framework-managed transactions
   * and converting JDBC SQLExceptions into Framework's DataAccessException hierarchy.
   * <p>The callback action can return a result object, for example a
   * domain object or a collection of domain objects.
   *
   * @param sql the SQL to execute
   * @param action callback object that specifies the action
   * @return a result object returned by the action, or {@code null}
   * @throws DataAccessException if there is any problem
   */
  @Nullable
  <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, reading the ResultSet with a
   * ResultSetExtractor.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param rse object that will extract results
   * @return an arbitrary result object, as returned by the ResultSetExtractor
   * @throws DataAccessException if the query fails
   */
  @Nullable
  <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, reading the ResultSet with a
   * ResultSetExtractor.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param rse object that will extract results
   * @return an arbitrary result object, as returned by the ResultSetExtractor
   * @throws DataAccessException if the query fails
   */
  @Nullable
  <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL,
   * reading the ResultSet with a ResultSetExtractor.
   * <p>Note: In contrast to the JdbcOperations method with the same signature,
   * this query variant always uses a PreparedStatement. It is effectively
   * equivalent to a query call with an empty parameter Map.
   *
   * @param sql the SQL query to execute
   * @param rse object that will extract results
   * @return an arbitrary result object, as returned by the ResultSetExtractor
   * @throws DataAccessException if the query fails
   */
  @Nullable
  <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list of
   * arguments to bind to the query, reading the ResultSet on a per-row basis
   * with a RowCallbackHandler.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param rch object that will extract results, one row at a time
   * @throws DataAccessException if the query fails
   */
  void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list of
   * arguments to bind to the query, reading the ResultSet on a per-row basis
   * with a RowCallbackHandler.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param rch object that will extract results, one row at a time
   * @throws DataAccessException if the query fails
   */
  void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL,
   * reading the ResultSet on a per-row basis with a RowCallbackHandler.
   * <p>Note: In contrast to the JdbcOperations method with the same signature,
   * this query variant always uses a PreparedStatement. It is effectively
   * equivalent to a query call with an empty parameter Map.
   *
   * @param sql the SQL query to execute
   * @param rch object that will extract results, one row at a time
   * @throws DataAccessException if the query fails
   */
  void query(String sql, RowCallbackHandler rch) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, mapping each row to a Java object
   * via a RowMapper.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param rowMapper object that will map one object per row
   * @return the result List, containing mapped objects
   * @throws DataAccessException if the query fails
   */
  <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, mapping each row to a Java object
   * via a RowMapper.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param rowMapper object that will map one object per row
   * @return the result List, containing mapped objects
   * @throws DataAccessException if the query fails
   */
  <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL,
   * mapping each row to a Java object via a RowMapper.
   * <p>Note: In contrast to the JdbcOperations method with the same signature,
   * this query variant always uses a PreparedStatement. It is effectively
   * equivalent to a query call with an empty parameter Map.
   *
   * @param sql the SQL query to execute
   * @param rowMapper object that will map one object per row
   * @return the result List, containing mapped objects
   * @throws DataAccessException if the query fails
   */
  <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, mapping each row to a Java object
   * via a RowMapper, and turning it into an iterable and closeable Stream.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param rowMapper object that will map one object per row
   * @return the result Stream, containing mapped objects, needing to be
   * closed once fully processed (e.g. through a try-with-resources clause)
   * @throws DataAccessException if the query fails
   */
  <T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, mapping each row to a Java object
   * via a RowMapper, and turning it into an iterable and closeable Stream.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param rowMapper object that will map one object per row
   * @return the result Stream, containing mapped objects, needing to be
   * closed once fully processed (e.g. through a try-with-resources clause)
   * @throws DataAccessException if the query fails
   */
  <T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, mapping a single result row to a
   * Java object via a RowMapper.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param rowMapper object that will map one object per row
   * @return the single mapped object (may be {@code null} if the given
   * {@link RowMapper} returned {@code} null)
   * @throws infra.dao.IncorrectResultSizeDataAccessException if the query does not return exactly one row
   * @throws DataAccessException if the query fails
   */
  @Nullable
  <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a list
   * of arguments to bind to the query, mapping a single result row to a
   * Java object via a RowMapper.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param rowMapper object that will map one object per row
   * @return the single mapped object (may be {@code null} if the given
   * {@link RowMapper} returned {@code} null)
   * @throws infra.dao.IncorrectResultSizeDataAccessException if the query does not return exactly one row
   * @throws DataAccessException if the query fails
   */
  @Nullable
  <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result object.
   * <p>The query is expected to be a single row/single column query; the returned
   * result will be directly mapped to the corresponding object type.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param requiredType the type that the result object is expected to match
   * @return the result object of the required type, or {@code null} in case of SQL NULL
   * @throws infra.dao.IncorrectResultSizeDataAccessException if the query does not return exactly one row
   * @throws IncorrectResultSetColumnCountException if the query does not return a row containing a single column
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForObject(String, Class)
   * @see SingleColumnRowMapper
   */
  @Nullable
  <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result object.
   * <p>The query is expected to be a single row/single column query; the returned
   * result will be directly mapped to the corresponding object type.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param requiredType the type that the result object is expected to match
   * @return the result object of the required type, or {@code null} in case of SQL NULL
   * @throws infra.dao.IncorrectResultSizeDataAccessException if the query does not return exactly one row
   * @throws IncorrectResultSetColumnCountException if the query does not return a row containing a single column
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForObject(String, Class)
   */
  @Nullable
  <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result Map.
   * <p>The query is expected to be a single row query; the result row will be
   * mapped to a Map (one entry for each column, using the column name as the key).
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @return the result Map (one entry for each column, using the column name as the key)
   * @throws infra.dao.IncorrectResultSizeDataAccessException if the query does not return exactly one row
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForMap(String)
   * @see ColumnMapRowMapper
   */
  Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result Map.
   * The queryForMap() methods defined by this interface are appropriate
   * when you don't have a domain model. Otherwise, consider using
   * one of the queryForObject() methods.
   * <p>The query is expected to be a single row query; the result row will be
   * mapped to a Map (one entry for each column, using the column name as the key).
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @return the result Map (one entry for each column, using the column name as the key)
   * @throws infra.dao.IncorrectResultSizeDataAccessException if the query does not return exactly one row
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForMap(String)
   * @see ColumnMapRowMapper
   */
  Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result list.
   * <p>The results will be mapped to a List (one entry for each row) of
   * result objects, each of them matching the specified element type.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @param elementType the required type of element in the result list
   * (for example, {@code Integer.class})
   * @return a List of objects that match the specified element type
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForList(String, Class)
   * @see SingleColumnRowMapper
   */
  <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result list.
   * <p>The results will be mapped to a List (one entry for each row) of
   * result objects, each of them matching the specified element type.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @param elementType the required type of element in the result list
   * (for example, {@code Integer.class})
   * @return a List of objects that match the specified element type
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForList(String, Class)
   * @see SingleColumnRowMapper
   */
  <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
          throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result list.
   * <p>The results will be mapped to a List (one entry for each row) of
   * Maps (one entry for each column, using the column name as the key).
   * Each element in the list will be of the form returned by this interface's
   * {@code queryForMap} methods.
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @return a List that contains a Map per row
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForList(String)
   */
  List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting a result list.
   * <p>The results will be mapped to a List (one entry for each row) of
   * Maps (one entry for each column, using the column name as the key).
   * Each element in the list will be of the form returned by this interface's
   * {@code queryForMap} methods.
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @return a List that contains a Map per row
   * @throws DataAccessException if the query fails
   * @see JdbcTemplate#queryForList(String)
   */
  List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting an SqlRowSet.
   * <p>The results will be mapped to an SqlRowSet which holds the data in a
   * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
   * <p>Note that, for the default implementation, JDBC RowSet support needs to
   * be available at runtime: by default, Sun's {@code com.sun.rowset.CachedRowSetImpl}
   * class is used, which is part of JDK 1.5+ and also available separately as part of
   * Sun's JDBC RowSet Implementations download (rowset.jar).
   *
   * @param sql the SQL query to execute
   * @param paramSource container of arguments to bind to the query
   * @return an SqlRowSet representation (possibly a wrapper around a
   * {@code javax.sql.rowset.CachedRowSet})
   * @throws DataAccessException if there is any problem executing the query
   * @see JdbcTemplate#queryForRowSet(String)
   * @see SqlRowSetResultSetExtractor
   * @see javax.sql.rowset.CachedRowSet
   */
  SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException;

  /**
   * Query given SQL to create a prepared statement from SQL and a
   * list of arguments to bind to the query, expecting an SqlRowSet.
   * <p>The results will be mapped to an SqlRowSet which holds the data in a
   * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
   * <p>Note that, for the default implementation, JDBC RowSet support needs to
   * be available at runtime: by default, Sun's {@code com.sun.rowset.CachedRowSetImpl}
   * class is used, which is part of JDK 1.5+ and also available separately as part of
   * Sun's JDBC RowSet Implementations download (rowset.jar).
   *
   * @param sql the SQL query to execute
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @return an SqlRowSet representation (possibly a wrapper around a
   * {@code javax.sql.rowset.CachedRowSet})
   * @throws DataAccessException if there is any problem executing the query
   * @see JdbcTemplate#queryForRowSet(String)
   * @see SqlRowSetResultSetExtractor
   * @see javax.sql.rowset.CachedRowSet
   */
  SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException;

  /**
   * Issue an update via a prepared statement, binding the given arguments.
   *
   * @param sql the SQL containing named parameters
   * @param paramSource container of arguments and SQL types to bind to the query
   * @return the number of rows affected
   * @throws DataAccessException if there is any problem issuing the update
   */
  int update(String sql, SqlParameterSource paramSource) throws DataAccessException;

  /**
   * Issue an update via a prepared statement, binding the given arguments.
   *
   * @param sql the SQL containing named parameters
   * @param paramMap map of parameters to bind to the query
   * (leaving it to the PreparedStatement to guess the corresponding SQL type)
   * @return the number of rows affected
   * @throws DataAccessException if there is any problem issuing the update
   */
  int update(String sql, Map<String, ?> paramMap) throws DataAccessException;

  /**
   * Issue an update via a prepared statement, binding the given arguments,
   * returning generated keys.
   *
   * @param sql the SQL containing named parameters
   * @param paramSource container of arguments and SQL types to bind to the query
   * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
   * @return the number of rows affected
   * @throws DataAccessException if there is any problem issuing the update
   * @see MapSqlParameterSource
   * @see GeneratedKeyHolder
   */
  int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
          throws DataAccessException;

  /**
   * Issue an update via a prepared statement, binding the given arguments,
   * returning generated keys.
   *
   * @param sql the SQL containing named parameters
   * @param paramSource container of arguments and SQL types to bind to the query
   * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
   * @param keyColumnNames names of the columns that will have keys generated for them
   * @return the number of rows affected
   * @throws DataAccessException if there is any problem issuing the update
   * @see MapSqlParameterSource
   * @see GeneratedKeyHolder
   */
  int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
          throws DataAccessException;

  /**
   * Executes a batch using the supplied SQL statement with the batch of supplied arguments.
   *
   * @param sql the SQL statement to execute
   * @param batchValues the array of Maps containing the batch of arguments for the query
   * @return an array containing the numbers of rows affected by each update in the batch
   * (may also contain special JDBC-defined negative values for affected rows such as
   * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
   * @throws DataAccessException if there is any problem issuing the update
   */
  int[] batchUpdate(String sql, Map<String, ?>[] batchValues);

  /**
   * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
   *
   * @param sql the SQL statement to execute
   * @param batchArgs the array of {@link SqlParameterSource} containing the batch of
   * arguments for the query
   * @return an array containing the numbers of rows affected by each update in the batch
   * (may also contain special JDBC-defined negative values for affected rows such as
   * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
   * @throws DataAccessException if there is any problem issuing the update
   */
  int[] batchUpdate(String sql, SqlParameterSource[] batchArgs);

  /**
   * Execute a batch using the supplied SQL statement with the batch of supplied
   * arguments, returning generated keys.
   *
   * @param sql the SQL statement to execute
   * @param batchArgs the array of {@link SqlParameterSource} containing the batch of
   * arguments for the query
   * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
   * @return an array containing the numbers of rows affected by each update in the batch
   * (may also contain special JDBC-defined negative values for affected rows such as
   * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
   * @throws DataAccessException if there is any problem issuing the update
   * @see GeneratedKeyHolder
   */
  int[] batchUpdate(String sql, SqlParameterSource[] batchArgs, KeyHolder generatedKeyHolder);

  /**
   * Execute a batch using the supplied SQL statement with the batch of supplied arguments,
   * returning generated keys.
   *
   * @param sql the SQL statement to execute
   * @param batchArgs the array of {@link SqlParameterSource} containing the batch of
   * arguments for the query
   * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
   * @param keyColumnNames names of the columns that will have keys generated for them
   * @return an array containing the numbers of rows affected by each update in the batch
   * (may also contain special JDBC-defined negative values for affected rows such as
   * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
   * @throws DataAccessException if there is any problem issuing the update
   * @see GeneratedKeyHolder
   */
  int[] batchUpdate(String sql, SqlParameterSource[] batchArgs, KeyHolder generatedKeyHolder,
          String[] keyColumnNames);
}
