/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.jdbc.datasource.DataSourceUtils;
import infra.lang.Assert;

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
