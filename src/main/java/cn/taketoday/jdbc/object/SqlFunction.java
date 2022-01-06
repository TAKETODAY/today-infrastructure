/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.dao.TypeMismatchDataAccessException;
import cn.taketoday.jdbc.core.SingleColumnRowMapper;
import cn.taketoday.lang.Nullable;

/**
 * SQL "function" wrapper for a query that returns a single row of results.
 * The default behavior is to return an int, but that can be overridden by
 * using the constructor with an extra return type parameter.
 *
 * <p>Intended to use to call SQL functions that return a single result using a
 * query like "select user()" or "select sysdate from dual". It is not intended
 * for calling more complex stored functions or for using a CallableStatement to
 * invoke a stored procedure or stored function. Use StoredProcedure or SqlCall
 * for this type of processing.
 *
 * <p>This is a concrete class, which there is often no need to subclass.
 * Code using this package can create an object of this type, declaring SQL
 * and parameters, and then invoke the appropriate {@code run} method
 * repeatedly to execute the function. Subclasses are only supposed to add
 * specialized {@code run} methods for specific parameter and return types.
 *
 * <p>Like all RdbmsOperation objects, SqlFunction objects are thread-safe.
 *
 * @param <T> the result type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see StoredProcedure
 */
public class SqlFunction<T> extends MappingSqlQuery<T> {

  private final SingleColumnRowMapper<T> rowMapper = new SingleColumnRowMapper<>();

  /**
   * Constructor to allow use as a JavaBean.
   * A DataSource, SQL and any parameters must be supplied before
   * invoking the {@code compile} method and using this object.
   *
   * @see #setDataSource
   * @see #setSql
   * @see #compile
   */
  public SqlFunction() {
    setRowsExpected(1);
  }

  /**
   * Create a new SqlFunction object with SQL, but without parameters.
   * Must add parameters or settle with none.
   *
   * @param ds the DataSource to obtain connections from
   * @param sql the SQL to execute
   */
  public SqlFunction(DataSource ds, String sql) {
    setRowsExpected(1);
    setDataSource(ds);
    setSql(sql);
  }

  /**
   * Create a new SqlFunction object with SQL and parameters.
   *
   * @param ds the DataSource to obtain connections from
   * @param sql the SQL to execute
   * @param types the SQL types of the parameters, as defined in the
   * {@code java.sql.Types} class
   * @see java.sql.Types
   */
  public SqlFunction(DataSource ds, String sql, int[] types) {
    setRowsExpected(1);
    setDataSource(ds);
    setSql(sql);
    setTypes(types);
  }

  /**
   * Create a new SqlFunction object with SQL, parameters and a result type.
   *
   * @param ds the DataSource to obtain connections from
   * @param sql the SQL to execute
   * @param types the SQL types of the parameters, as defined in the
   * {@code java.sql.Types} class
   * @param resultType the type that the result object is required to match
   * @see #setResultType(Class)
   * @see java.sql.Types
   */
  public SqlFunction(DataSource ds, String sql, int[] types, Class<T> resultType) {
    setRowsExpected(1);
    setDataSource(ds);
    setSql(sql);
    setTypes(types);
    setResultType(resultType);
  }

  /**
   * Specify the type that the result object is required to match.
   * <p>If not specified, the result value will be exposed as
   * returned by the JDBC driver.
   */
  public void setResultType(Class<T> resultType) {
    this.rowMapper.setRequiredType(resultType);
  }

  /**
   * This implementation of this method extracts a single value from the
   * single row returned by the function. If there are a different number
   * of rows returned, this is treated as an error.
   */
  @Override
  @Nullable
  protected T mapRow(ResultSet rs, int rowNum) throws SQLException {
    return this.rowMapper.mapRow(rs, rowNum);
  }

  /**
   * Convenient method to run the function without arguments.
   *
   * @return the value of the function
   */
  public int run() {
    return run(new Object[0]);
  }

  /**
   * Convenient method to run the function with a single int argument.
   *
   * @param parameter single int parameter
   * @return the value of the function
   */
  public int run(int parameter) {
    return run(new Object[] { parameter });
  }

  /**
   * Analogous to the SqlQuery.execute([]) method. This is a
   * generic method to execute a query, taken a number of arguments.
   *
   * @param parameters array of parameters. These will be objects or
   * object wrapper types for primitives.
   * @return the value of the function
   */
  public int run(Object... parameters) {
    Object obj = super.findObject(parameters);
    if (!(obj instanceof Number)) {
      throw new TypeMismatchDataAccessException("Could not convert result object [" + obj + "] to int");
    }
    return ((Number) obj).intValue();
  }

  /**
   * Convenient method to run the function without arguments,
   * returning the value as an object.
   *
   * @return the value of the function
   */
  @Nullable
  public Object runGeneric() {
    return findObject((Object[]) null, null);
  }

  /**
   * Convenient method to run the function with a single int argument.
   *
   * @param parameter single int parameter
   * @return the value of the function as an Object
   */
  @Nullable
  public Object runGeneric(int parameter) {
    return findObject(parameter);
  }

  /**
   * Analogous to the {@code SqlQuery.findObject(Object[])} method.
   * This is a generic method to execute a query, taken a number of arguments.
   *
   * @param parameters array of parameters. These will be objects or
   * object wrapper types for primitives.
   * @return the value of the function, as an Object
   * @see #execute(Object[])
   */
  @Nullable
  public Object runGeneric(Object[] parameters) {
    return findObject(parameters);
  }

}
