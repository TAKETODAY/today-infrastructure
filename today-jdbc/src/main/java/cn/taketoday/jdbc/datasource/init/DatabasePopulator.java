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

package cn.taketoday.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Assert;

/**
 * Strategy used to populate, initialize, or clean up a database.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @see ResourceDatabasePopulator
 * @see DataSourceInitializer
 * @since 4.0
 */
@FunctionalInterface
public interface DatabasePopulator {

  /**
   * Populate, initialize, or clean up the database using the provided JDBC
   * connection.
   * <p><strong>Warning</strong>: Concrete implementations should not close
   * the provided {@link Connection}.
   * <p>Concrete implementations <em>may</em> throw an {@link SQLException} if
   * an error is encountered but are <em>strongly encouraged</em> to throw a
   * specific {@link ScriptException} instead. For example, Framework's
   * {@link ResourceDatabasePopulator} and {@link DatabasePopulator#execute} wrap
   * all {@code SQLExceptions} in {@code ScriptExceptions}.
   *
   * @param connection the JDBC connection to use; already configured and
   * ready to use; never {@code null}
   * @throws SQLException if an unrecoverable data access exception occurs
   * while interacting with the database
   * @throws ScriptException in all other error cases
   * @see DatabasePopulator#execute
   */
  void populate(Connection connection) throws SQLException, ScriptException;

  /**
   * Execute the given {@link DatabasePopulator} against the given {@link DataSource}.
   * <p>the {@link Connection} for the supplied
   * {@code DataSource} will be {@linkplain Connection#commit() committed} if
   * it is not configured for {@link Connection#getAutoCommit() auto-commit} and
   * is not {@linkplain DataSourceUtils#isConnectionTransactional transactional}.
   *
   * @param populator the {@code DatabasePopulator} to execute
   * @param dataSource the {@code DataSource} to execute against
   * @throws DataAccessException if an error occurs, specifically a {@link ScriptException}
   * @see DataSourceUtils#isConnectionTransactional(Connection, DataSource)
   */
  static void execute(DatabasePopulator populator, DataSource dataSource) throws DataAccessException {
    Assert.notNull(populator, "DatabasePopulator is required");
    Assert.notNull(dataSource, "DataSource is required");
    try {
      Connection connection = DataSourceUtils.getConnection(dataSource);
      try {
        populator.populate(connection);
        if (!connection.getAutoCommit() && !DataSourceUtils.isConnectionTransactional(connection, dataSource)) {
          connection.commit();
        }
      }
      finally {
        DataSourceUtils.releaseConnection(connection, dataSource);
      }
    }
    catch (ScriptException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new UncategorizedScriptException("Failed to execute database script", ex);
    }
  }

}
