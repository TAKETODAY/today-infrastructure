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

import java.sql.CallableStatement;
import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Generic callback interface for code that operates on a CallableStatement.
 * Allows to execute any number of operations on a single CallableStatement,
 * for example a single execute call or repeated execute calls with varying
 * parameters.
 *
 * <p>Used internally by JdbcTemplate, but also useful for application code.
 * Note that the passed-in CallableStatement can have been created by the
 * framework or by a custom CallableStatementCreator. However, the latter is
 * hardly ever necessary, as most custom callback actions will perform updates
 * in which case a standard CallableStatement is fine. Custom actions will
 * always set parameter values themselves, so that CallableStatementCreator
 * capability is not needed either.
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @see JdbcTemplate#execute(String, CallableStatementCallback)
 * @see JdbcTemplate#execute(CallableStatementCreator, CallableStatementCallback)
 * @since 4.0
 */
@FunctionalInterface
public interface CallableStatementCallback<T> {

  /**
   * Gets called by {@code JdbcTemplate.execute} with an active JDBC
   * CallableStatement. Does not need to care about closing the Statement
   * or the Connection, or about handling transactions: this will all be
   * handled by Framework's JdbcTemplate.
   *
   * <p><b>NOTE:</b> Any ResultSets opened should be closed in finally blocks
   * within the callback implementation. Framework will close the Statement
   * object after the callback returned, but this does not necessarily imply
   * that the ResultSet resources will be closed: the Statement objects might
   * get pooled by the connection pool, with {@code close} calls only
   * returning the object to the pool but not physically closing the resources.
   *
   * <p>If called without a thread-bound JDBC transaction (initiated by
   * DataSourceTransactionManager), the code will simply get executed on the
   * JDBC connection with its transactional semantics. If JdbcTemplate is
   * configured to use a JTA-aware DataSource, the JDBC connection and thus
   * the callback code will be transactional if a JTA transaction is active.
   *
   * <p>Allows for returning a result object created within the callback, i.e.
   * a domain object or a collection of domain objects. A thrown RuntimeException
   * is treated as application exception: it gets propagated to the caller of
   * the template.
   *
   * @param cs active JDBC CallableStatement
   * @return a result object, or {@code null} if none
   * @throws SQLException if thrown by a JDBC method, to be auto-converted
   * into a DataAccessException by an SQLExceptionTranslator
   * @throws DataAccessException in case of custom exceptions
   */
  @Nullable
  T doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException;

}
