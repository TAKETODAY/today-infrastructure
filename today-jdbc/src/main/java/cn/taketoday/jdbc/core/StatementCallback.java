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

package cn.taketoday.jdbc.core;

import java.sql.SQLException;
import java.sql.Statement;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Generic callback interface for code that operates on a JDBC Statement.
 * Allows to execute any number of operations on a single Statement,
 * for example a single {@code executeUpdate} call or repeated
 * {@code executeUpdate} calls with varying SQL.
 *
 * <p>Used internally by JdbcTemplate, but also useful for application code.
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @see JdbcTemplate#execute(StatementCallback)
 * @since 4.0
 */
@FunctionalInterface
public interface StatementCallback<T> {

  /**
   * Gets called by {@code JdbcTemplate.execute} with an active JDBC
   * Statement. Does not need to care about closing the Statement or the
   * Connection, or about handling transactions: this will all be handled
   * by Framework's JdbcTemplate.
   * <p><b>NOTE:</b> Any ResultSets opened should be closed in finally blocks
   * within the callback implementation. Framework will close the Statement
   * object after the callback returned, but this does not necessarily imply
   * that the ResultSet resources will be closed: the Statement objects might
   * get pooled by the connection pool, with {@code close} calls only
   * returning the object to the pool but not physically closing the resources.
   * <p>If called without a thread-bound JDBC transaction (initiated by
   * DataSourceTransactionManager), the code will simply get executed on the
   * JDBC connection with its transactional semantics. If JdbcTemplate is
   * configured to use a JTA-aware DataSource, the JDBC connection and thus
   * the callback code will be transactional if a JTA transaction is active.
   * <p>Allows for returning a result object created within the callback, i.e.
   * a domain object or a collection of domain objects. Note that there's
   * special support for single step actions: see JdbcTemplate.queryForObject etc.
   * A thrown RuntimeException is treated as application exception, it gets
   * propagated to the caller of the template.
   *
   * @param stmt active JDBC Statement
   * @return a result object, or {@code null} if none
   * @throws SQLException if thrown by a JDBC method, to be auto-converted
   * to a DataAccessException by an SQLExceptionTranslator
   * @throws DataAccessException in case of custom exceptions
   * @see JdbcTemplate#queryForObject(String, Class)
   * @see JdbcTemplate#queryForRowSet(String)
   */
  @Nullable
  T doInStatement(Statement stmt) throws SQLException, DataAccessException;

}
