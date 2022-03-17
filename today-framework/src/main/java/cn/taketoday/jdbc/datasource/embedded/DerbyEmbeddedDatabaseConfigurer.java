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

import org.apache.derby.jdbc.EmbeddedDriver;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link EmbeddedDatabaseConfigurer} for the Apache Derby database.
 *
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 4.0
 */
final class DerbyEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

  private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

  @Nullable
  private static DerbyEmbeddedDatabaseConfigurer instance;

  /**
   * Get the singleton {@link DerbyEmbeddedDatabaseConfigurer} instance.
   *
   * @return the configurer instance
   */
  public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() {
    if (instance == null) {
      // disable log file
      System.setProperty("derby.stream.error.method",
              OutputStreamFactory.class.getName() + ".getNoopOutputStream");
      instance = new DerbyEmbeddedDatabaseConfigurer();
    }
    return instance;
  }

  private DerbyEmbeddedDatabaseConfigurer() {
  }

  @Override
  public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
    properties.setDriverClass(EmbeddedDriver.class);
    properties.setUrl(String.format(URL_TEMPLATE, databaseName, "create=true"));
    properties.setUsername("sa");
    properties.setPassword("");
  }

  @Override
  public void shutdown(DataSource dataSource, String databaseName) {
    try {
      new EmbeddedDriver().connect(
              String.format(URL_TEMPLATE, databaseName, "drop=true"), new Properties());
    }
    catch (SQLException ex) {
      // Error code that indicates successful shutdown
      if (!"08006".equals(ex.getSQLState())) {
        LoggerFactory.getLogger(getClass()).warn("Could not shut down embedded Derby database", ex);
      }
    }
  }

}
