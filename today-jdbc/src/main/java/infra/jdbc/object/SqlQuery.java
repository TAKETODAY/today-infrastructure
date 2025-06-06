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

package infra.jdbc.object;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.dao.support.DataAccessUtils;
import infra.jdbc.core.RowMapper;
import infra.jdbc.core.namedparam.MapSqlParameterSource;
import infra.jdbc.core.namedparam.NamedParameterUtils;
import infra.jdbc.core.namedparam.ParsedSql;
import infra.lang.Nullable;

/**
 * Reusable operation object representing an SQL query.
 *
 * <p>Subclasses must implement the {@link #newRowMapper} method to provide
 * an object that can extract the results of iterating over the
 * {@code ResultSet} created during the execution of the query.
 *
 * <p>This class provides a number of public {@code execute} methods that are
 * analogous to the different convenient JDO query execute methods. Subclasses
 * can either rely on one of these inherited methods, or can add their own
 * custom execution methods, with meaningful names and typed parameters
 * (definitely a best practice). Each custom query method will invoke one of
 * this class's untyped query methods.
 *
 * <p>Like all {@code RdbmsOperation} classes that ship with the
 * Framework, {@code SqlQuery} instances are thread-safe after their
 * initialization is complete. That is, after they are constructed and configured
 * via their setter methods, they can be used safely from multiple threads.
 *
 * @param <T> the result type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @see SqlUpdate
 */
public abstract class SqlQuery<T> extends SqlOperation {

  /** The number of rows to expect; if 0, unknown. */
  private int rowsExpected = 0;

  /**
   * Constructor to allow use as a JavaBean.
   * <p>The {@code DataSource} and SQL must be supplied before
   * compilation and use.
   */
  public SqlQuery() { }

  /**
   * Convenient constructor with a {@code DataSource} and SQL string.
   *
   * @param ds the {@code DataSource} to use to get connections
   * @param sql the SQL to execute; SQL can also be supplied at runtime
   * by overriding the {@link #getSql()} method.
   */
  public SqlQuery(DataSource ds, String sql) {
    setDataSource(ds);
    setSql(sql);
  }

  /**
   * Set the number of rows expected.
   * <p>This can be used to ensure efficient storage of results. The
   * default behavior is not to expect any specific number of rows.
   */
  public void setRowsExpected(int rowsExpected) {
    this.rowsExpected = rowsExpected;
  }

  /**
   * Get the number of rows expected.
   */
  public int getRowsExpected() {
    return this.rowsExpected;
  }

  /**
   * Central execution method. All un-named parameter execution goes through this method.
   *
   * @param params parameters, similar to JDO query parameters.
   * Primitive parameters must be represented by their Object wrapper type.
   * The ordering of parameters is significant.
   * @param context the contextual information passed to the {@code mapRow}
   * callback method. The JDBC operation itself doesn't rely on this parameter,
   * but it can be useful for creating the objects of the result list.
   * @return a List of objects, one per row of the ResultSet. Normally all these
   * will be of the same class, although it is possible to use different types.
   */
  public List<T> execute(@Nullable Object[] params, @Nullable Map<?, ?> context) throws DataAccessException {
    validateParameters(params);
    RowMapper<T> rowMapper = newRowMapper(params, context);
    return getJdbcTemplate().query(newPreparedStatementCreator(params), rowMapper);
  }

  /**
   * Convenient method to execute without context.
   *
   * @param params parameters for the query. Primitive parameters must
   * be represented by their Object wrapper type. The ordering of parameters is
   * significant.
   */
  public List<T> execute(Object... params) throws DataAccessException {
    return execute(params, null);
  }

  /**
   * Convenient method to execute without parameters.
   *
   * @param context the contextual information for object creation
   */
  public List<T> execute(Map<?, ?> context) throws DataAccessException {
    return execute((Object[]) null, context);
  }

  /**
   * Convenient method to execute without parameters nor context.
   */
  public List<T> execute() throws DataAccessException {
    return execute((Object[]) null, null);
  }

  /**
   * Convenient method to execute with a single int parameter and context.
   *
   * @param p1 single int parameter
   * @param context the contextual information for object creation
   */
  public List<T> execute(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
    return execute(new Object[] { p1 }, context);
  }

  /**
   * Convenient method to execute with a single int parameter.
   *
   * @param p1 single int parameter
   */
  public List<T> execute(int p1) throws DataAccessException {
    return execute(p1, null);
  }

  /**
   * Convenient method to execute with two int parameters and context.
   *
   * @param p1 first int parameter
   * @param p2 second int parameter
   * @param context the contextual information for object creation
   */
  public List<T> execute(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
    return execute(new Object[] { p1, p2 }, context);
  }

  /**
   * Convenient method to execute with two int parameters.
   *
   * @param p1 first int parameter
   * @param p2 second int parameter
   */
  public List<T> execute(int p1, int p2) throws DataAccessException {
    return execute(p1, p2, null);
  }

  /**
   * Convenient method to execute with a single long parameter and context.
   *
   * @param p1 single long parameter
   * @param context the contextual information for object creation
   */
  public List<T> execute(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
    return execute(new Object[] { p1 }, context);
  }

  /**
   * Convenient method to execute with a single long parameter.
   *
   * @param p1 single long parameter
   */
  public List<T> execute(long p1) throws DataAccessException {
    return execute(p1, null);
  }

  /**
   * Convenient method to execute with a single String parameter and context.
   *
   * @param p1 single String parameter
   * @param context the contextual information for object creation
   */
  public List<T> execute(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
    return execute(new Object[] { p1 }, context);
  }

  /**
   * Convenient method to execute with a single String parameter.
   *
   * @param p1 single String parameter
   */
  public List<T> execute(String p1) throws DataAccessException {
    return execute(p1, null);
  }

  /**
   * Central execution method. All named parameter execution goes through this method.
   *
   * @param paramMap parameters associated with the name specified while declaring
   * the SqlParameters. Primitive parameters must be represented by their Object wrapper
   * type. The ordering of parameters is not significant since they are supplied in a
   * SqlParameterMap which is an implementation of the Map interface.
   * @param context the contextual information passed to the {@code mapRow}
   * callback method. The JDBC operation itself doesn't rely on this parameter,
   * but it can be useful for creating the objects of the result list.
   * @return a List of objects, one per row of the ResultSet. Normally all these
   * will be of the same class, although it is possible to use different types.
   */
  public List<T> executeByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
    validateNamedParameters(paramMap);
    ParsedSql parsedSql = getParsedSql();
    MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
    String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
    Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
    RowMapper<T> rowMapper = newRowMapper(params, context);
    return getJdbcTemplate().query(newPreparedStatementCreator(sqlToUse, params), rowMapper);
  }

  /**
   * Convenient method to execute without context.
   *
   * @param paramMap parameters associated with the name specified while declaring
   * the SqlParameters. Primitive parameters must be represented by their Object wrapper
   * type. The ordering of parameters is not significant.
   */
  public List<T> executeByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
    return executeByNamedParam(paramMap, null);
  }

  /**
   * Generic object finder method, used by all other {@code findObject} methods.
   * Object finder methods are like EJB entity bean finders, in that it is
   * considered an error if they return more than one result.
   *
   * @return the result object, or {@code null} if not found. Subclasses may
   * choose to treat this as an error and throw an exception.
   * @see infra.dao.support.DataAccessUtils#singleResult
   */
  @Nullable
  public T findObject(@Nullable Object[] params, @Nullable Map<?, ?> context) throws DataAccessException {
    List<T> results = execute(params, context);
    return DataAccessUtils.singleResult(results);
  }

  /**
   * Convenient method to find a single object without context.
   */
  @Nullable
  public T findObject(Object... params) throws DataAccessException {
    return findObject(params, null);
  }

  /**
   * Convenient method to find a single object given a single int parameter
   * and a context.
   */
  @Nullable
  public T findObject(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
    return findObject(new Object[] { p1 }, context);
  }

  /**
   * Convenient method to find a single object given a single int parameter.
   */
  @Nullable
  public T findObject(int p1) throws DataAccessException {
    return findObject(p1, null);
  }

  /**
   * Convenient method to find a single object given two int parameters
   * and a context.
   */
  @Nullable
  public T findObject(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
    return findObject(new Object[] { p1, p2 }, context);
  }

  /**
   * Convenient method to find a single object given two int parameters.
   */
  @Nullable
  public T findObject(int p1, int p2) throws DataAccessException {
    return findObject(p1, p2, null);
  }

  /**
   * Convenient method to find a single object given a single long parameter
   * and a context.
   */
  @Nullable
  public T findObject(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
    return findObject(new Object[] { p1 }, context);
  }

  /**
   * Convenient method to find a single object given a single long parameter.
   */
  @Nullable
  public T findObject(long p1) throws DataAccessException {
    return findObject(p1, null);
  }

  /**
   * Convenient method to find a single object given a single String parameter
   * and a context.
   */
  @Nullable
  public T findObject(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
    return findObject(new Object[] { p1 }, context);
  }

  /**
   * Convenient method to find a single object given a single String parameter.
   */
  @Nullable
  public T findObject(String p1) throws DataAccessException {
    return findObject(p1, null);
  }

  /**
   * Generic object finder method for named parameters.
   *
   * @param paramMap a Map of parameter name to parameter object,
   * matching named parameters specified in the SQL statement.
   * Ordering is not significant.
   * @param context the contextual information passed to the {@code mapRow}
   * callback method. The JDBC operation itself doesn't rely on this parameter,
   * but it can be useful for creating the objects of the result list.
   * @return a List of objects, one per row of the ResultSet. Normally all these
   * will be of the same class, although it is possible to use different types.
   */
  @Nullable
  public T findObjectByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
    List<T> results = executeByNamedParam(paramMap, context);
    return DataAccessUtils.singleResult(results);
  }

  /**
   * Convenient method to execute without context.
   *
   * @param paramMap a Map of parameter name to parameter object,
   * matching named parameters specified in the SQL statement.
   * Ordering is not significant.
   */
  @Nullable
  public T findObjectByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
    return findObjectByNamedParam(paramMap, null);
  }

  /**
   * Subclasses must implement this method to extract an object per row, to be
   * returned by the {@code execute} method as an aggregated {@link List}.
   *
   * @param parameters the parameters to the {@code execute()} method,
   * in case subclass is interested; may be {@code null} if there
   * were no parameters.
   * @param context the contextual information passed to the {@code mapRow}
   * callback method. The JDBC operation itself doesn't rely on this parameter,
   * but it can be useful for creating the objects of the result list.
   * @see #execute
   */
  protected abstract RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context);

}
