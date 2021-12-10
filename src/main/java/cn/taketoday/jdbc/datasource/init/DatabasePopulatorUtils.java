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

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Assert;

/**
 * Utility methods for executing a {@link DatabasePopulator}.
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class DatabasePopulatorUtils {

  /**
   * Execute the given {@link DatabasePopulator} against the given {@link DataSource}.
   * <p>As of Framework Framework 5.3.11, the {@link Connection} for the supplied
   * {@code DataSource} will be {@linkplain Connection#commit() committed} if
   * it is not configured for {@link Connection#getAutoCommit() auto-commit} and
   * is not {@linkplain DataSourceUtils#isConnectionTransactional transactional}.
   *
   * @param populator the {@code DatabasePopulator} to execute
   * @param dataSource the {@code DataSource} to execute against
   * @throws DataAccessException if an error occurs, specifically a {@link ScriptException}
   * @see DataSourceUtils#isConnectionTransactional(Connection, DataSource)
   */
  public static void execute(DatabasePopulator populator, DataSource dataSource) throws DataAccessException {
    Assert.notNull(populator, "DatabasePopulator must not be null");
    Assert.notNull(dataSource, "DataSource must not be null");
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
