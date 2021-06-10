/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.jdbc.support;

import java.sql.SQLException;

/**
 * @author TODAY <br>
 * 2019-08-18 20:07
 */
public interface BasicOperation {

  // Execute
  // ------------------------------------------------

  /**
   * Execute a JDBC data access operation, implemented as callback action working
   * on a JDBC Connection. This allows for implementing arbitrary data access
   * operations
   * <p>
   * The callback action can return a result object, for example a domain object
   * or a collection of domain objects.
   *
   * @param action
   *         the callback object that specifies the action
   *
   * @return a result object returned by the action, or {@code null} @ if there is
   * any problem
   */
  <T> T execute(ConnectionCallback<T> action) throws SQLException;

  /**
   * Execute a JDBC data access operation, implemented as callback action working
   * on a JDBC Statement. This allows for implementing arbitrary data access
   * operations on a single Statement, within Spring's managed JDBC environment:
   * that is, participating in Spring-managed transactions and converting JDBC
   * SQLExceptions into Spring's DataAccessException hierarchy.
   * <p>
   * The callback action can return a result object, for example a domain object
   * or a collection of domain objects.
   *
   * @param action
   *         callback object that specifies the action
   *
   * @return a result object returned by the action, or {@code null} @ if there is
   * any problem
   */
  <T> T execute(StatementCallback<T> action) throws SQLException;

  /**
   * Issue a single SQL execute, typically a DDL statement.
   *
   * @param sql
   *         static SQL to execute @ if there is any problem
   */
  void execute(String sql) throws SQLException;

  /**
   * Execute a JDBC data access operation, implemented as callback action working
   * on a JDBC PreparedStatement. This allows for implementing arbitrary data
   * access operations on a single Statement, within Spring's managed JDBC
   * environment: that is, participating in Spring-managed transactions and
   * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
   * <p>
   * The callback action can return a result object, for example a domain object
   * or a collection of domain objects.
   *
   * @param sql
   *         SQL to execute
   * @param action
   *         callback object that specifies the action
   *
   * @return a result object returned by the action, or {@code null} @ if there is
   * any problem
   */
  <T> T execute(String sql, PreparedStatementCallback<T> action) throws SQLException;

  /**
   * Execute a JDBC data access operation, implemented as callback action working
   * on a JDBC CallableStatement. This allows for implementing arbitrary data
   * access operations on a single Statement, within Spring's managed JDBC
   * environment: that is, participating in Spring-managed transactions and
   * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
   * <p>
   * The callback action can return a result object, for example a domain object
   * or a collection of domain objects.
   *
   * @param sql
   *         the SQL call string to execute
   * @param action
   *         callback object that specifies the action
   *
   * @return a result object returned by the action, or {@code null} @ if there is
   * any problem
   */
  <T> T execute(String sql, CallableStatementCallback<T> action) throws SQLException;

}
