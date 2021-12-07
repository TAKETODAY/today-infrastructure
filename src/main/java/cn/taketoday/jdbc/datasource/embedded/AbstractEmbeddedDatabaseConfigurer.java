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

package cn.taketoday.jdbc.datasource.embedded;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for {@link EmbeddedDatabaseConfigurer} implementations
 * providing common shutdown behavior through a "SHUTDOWN" statement.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 4.0
 */
abstract class AbstractEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void shutdown(DataSource dataSource, String databaseName) {
    Connection con = null;
    try {
      con = dataSource.getConnection();
      if (con != null) {
        try (Statement stmt = con.createStatement()) {
          stmt.execute("SHUTDOWN");
        }
      }
    }
    catch (SQLException ex) {
      logger.info("Could not shut down embedded database", ex);
    }
    finally {
      if (con != null) {
        try {
          con.close();
        }
        catch (SQLException ex) {
          logger.debug("Could not close JDBC Connection on shutdown", ex);
        }
        catch (Throwable ex) {
          logger.debug("Unexpected exception on closing JDBC Connection", ex);
        }
      }
    }
  }

}
