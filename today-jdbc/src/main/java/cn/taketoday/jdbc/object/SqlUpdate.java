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

package cn.taketoday.jdbc.object;

import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import cn.taketoday.jdbc.core.namedparam.MapSqlParameterSource;
import cn.taketoday.jdbc.core.namedparam.NamedParameterUtils;
import cn.taketoday.jdbc.core.namedparam.ParsedSql;
import cn.taketoday.jdbc.support.KeyHolder;

/**
 * Reusable operation object representing an SQL update.
 *
 * <p>This class provides a number of {@code update} methods,
 * analogous to the {@code execute} methods of query objects.
 *
 * <p>This class is concrete. Although it can be subclassed (for example
 * to add a custom update method) it can easily be parameterized by setting
 * SQL and declaring parameters.
 *
 * <p>Like all {@code RdbmsOperation} classes that ship with the
 * Framework, {@code SqlQuery} instances are thread-safe after their
 * initialization is complete. That is, after they are constructed and configured
 * via their setter methods, they can be used safely from multiple threads.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SqlQuery
 * @since 4.0
 */
public class SqlUpdate extends SqlOperation {

  /**
   * Maximum number of rows the update may affect. If more are
   * affected, an exception will be thrown. Ignored if 0.
   */
  private int maxRowsAffected = 0;

  /**
   * An exact number of rows that must be affected.
   * Ignored if 0.
   */
  private int requiredRowsAffected = 0;

  /**
   * Constructor to allow use as a JavaBean. DataSource and SQL
   * must be supplied before compilation and use.
   *
   * @see #setDataSource
   * @see #setSql
   */
  public SqlUpdate() {
  }

  /**
   * Constructs an update object with a given DataSource and SQL.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL statement to execute
   */
  public SqlUpdate(DataSource ds, String sql) {
    setDataSource(ds);
    setSql(sql);
  }

  /**
   * Construct an update object with a given DataSource, SQL
   * and anonymous parameters.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL statement to execute
   * @param types the SQL types of the parameters, as defined in the
   * {@code java.sql.Types} class
   * @see java.sql.Types
   */
  public SqlUpdate(DataSource ds, String sql, int[] types) {
    setDataSource(ds);
    setSql(sql);
    setTypes(types);
  }

  /**
   * Construct an update object with a given DataSource, SQL,
   * anonymous parameters and specifying the maximum number of rows
   * that may be affected.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL statement to execute
   * @param types the SQL types of the parameters, as defined in the
   * {@code java.sql.Types} class
   * @param maxRowsAffected the maximum number of rows that may
   * be affected by the update
   * @see java.sql.Types
   */
  public SqlUpdate(DataSource ds, String sql, int[] types, int maxRowsAffected) {
    setDataSource(ds);
    setSql(sql);
    setTypes(types);
    this.maxRowsAffected = maxRowsAffected;
  }

  /**
   * Set the maximum number of rows that may be affected by this update.
   * The default value is 0, which does not limit the number of rows affected.
   *
   * @param maxRowsAffected the maximum number of rows that can be affected by
   * this update without this class's update method considering it an error
   */
  public void setMaxRowsAffected(int maxRowsAffected) {
    this.maxRowsAffected = maxRowsAffected;
  }

  /**
   * Set the <i>exact</i> number of rows that must be affected by this update.
   * The default value is 0, which allows any number of rows to be affected.
   * <p>This is an alternative to setting the <i>maximum</i> number of rows
   * that may be affected.
   *
   * @param requiredRowsAffected the exact number of rows that must be affected
   * by this update without this class's update method considering it an error
   */
  public void setRequiredRowsAffected(int requiredRowsAffected) {
    this.requiredRowsAffected = requiredRowsAffected;
  }

  /**
   * Check the given number of affected rows against the
   * specified maximum number or required number.
   *
   * @param rowsAffected the number of affected rows
   * @throws JdbcUpdateAffectedIncorrectNumberOfRowsException if the actually affected rows are out of bounds
   * @see #setMaxRowsAffected
   * @see #setRequiredRowsAffected
   */
  protected void checkRowsAffected(int rowsAffected) throws JdbcUpdateAffectedIncorrectNumberOfRowsException {
    if (this.maxRowsAffected > 0 && rowsAffected > this.maxRowsAffected) {
      throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(resolveSql(), this.maxRowsAffected, rowsAffected);
    }
    if (this.requiredRowsAffected > 0 && rowsAffected != this.requiredRowsAffected) {
      throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(resolveSql(), this.requiredRowsAffected, rowsAffected);
    }
  }

  /**
   * Generic method to execute the update given parameters.
   * All other update methods invoke this method.
   *
   * @param params array of parameters objects
   * @return the number of rows affected by the update
   */
  public int update(Object... params) throws DataAccessException {
    validateParameters(params);
    int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(params));
    checkRowsAffected(rowsAffected);
    return rowsAffected;
  }

  /**
   * Method to execute the update given arguments and
   * retrieve the generated keys using a KeyHolder.
   *
   * @param params array of parameter objects
   * @param generatedKeyHolder the KeyHolder that will hold the generated keys
   * @return the number of rows affected by the update
   */
  public int update(Object[] params, KeyHolder generatedKeyHolder) throws DataAccessException {
    if (!isReturnGeneratedKeys() && getGeneratedKeysColumnNames() == null) {
      throw new InvalidDataAccessApiUsageException(
              "The update method taking a KeyHolder should only be used when generated keys have " +
                      "been configured by calling either 'setReturnGeneratedKeys' or " +
                      "'setGeneratedKeysColumnNames'.");
    }
    validateParameters(params);
    int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(params), generatedKeyHolder);
    checkRowsAffected(rowsAffected);
    return rowsAffected;
  }

  /**
   * Convenience method to execute an update with no parameters.
   */
  public int update() throws DataAccessException {
    return update(new Object[0]);
  }

  /**
   * Convenient method to execute an update given one int arg.
   */
  public int update(int p1) throws DataAccessException {
    return update(new Object[] { p1 });
  }

  /**
   * Convenient method to execute an update given two int args.
   */
  public int update(int p1, int p2) throws DataAccessException {
    return update(new Object[] { p1, p2 });
  }

  /**
   * Convenient method to execute an update given one long arg.
   */
  public int update(long p1) throws DataAccessException {
    return update(new Object[] { p1 });
  }

  /**
   * Convenient method to execute an update given two long args.
   */
  public int update(long p1, long p2) throws DataAccessException {
    return update(new Object[] { p1, p2 });
  }

  /**
   * Convenient method to execute an update given one String arg.
   */
  public int update(String p) throws DataAccessException {
    return update(new Object[] { p });
  }

  /**
   * Convenient method to execute an update given two String args.
   */
  public int update(String p1, String p2) throws DataAccessException {
    return update(new Object[] { p1, p2 });
  }

  /**
   * Generic method to execute the update given named parameters.
   * All other update methods invoke this method.
   *
   * @param paramMap a Map of parameter name to parameter object,
   * matching named parameters specified in the SQL statement
   * @return the number of rows affected by the update
   */
  public int updateByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
    validateNamedParameters(paramMap);
    ParsedSql parsedSql = getParsedSql();
    MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
    String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
    Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
    int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(sqlToUse, params));
    checkRowsAffected(rowsAffected);
    return rowsAffected;
  }

  /**
   * Method to execute the update given arguments and
   * retrieve the generated keys using a KeyHolder.
   *
   * @param paramMap a Map of parameter name to parameter object,
   * matching named parameters specified in the SQL statement
   * @param generatedKeyHolder the KeyHolder that will hold the generated keys
   * @return the number of rows affected by the update
   */
  public int updateByNamedParam(Map<String, ?> paramMap, KeyHolder generatedKeyHolder) throws DataAccessException {
    validateNamedParameters(paramMap);
    ParsedSql parsedSql = getParsedSql();
    MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
    String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
    Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
    int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(sqlToUse, params), generatedKeyHolder);
    checkRowsAffected(rowsAffected);
    return rowsAffected;
  }

}
