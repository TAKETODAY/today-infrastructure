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

package cn.taketoday.jdbc.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Properties;

/**
 * use DriverManager to establish a connection
 *
 * @author TODAY 2021/6/10 22:29
 */
final class GenericConnectionSource implements ConnectionSource {
  private final String url;
  private final Properties properties;

  GenericConnectionSource(String url, Properties properties) {
    this.url = fixUrl(url);
    this.properties = properties;
  }

  GenericConnectionSource(String url, String user, String password) {
    this(url, new Properties());
    if (user != null) {
      properties.put("user", user);
    }
    if (password != null) {
      properties.put("password", password);
    }
  }

  private static String fixUrl(String url) {
    if (url.startsWith("jdbc")) {
      return url;
    }
    return "jdbc:".concat(url);
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return properties.getProperty("user");
  }

  public String getPassword() {
    return properties.getProperty("password");
  }

  /**
   * Attempts to establish a connection to the given database URL.
   * The <code>DriverManager</code> attempts to select an appropriate driver from
   * the set of registered JDBC drivers.
   * <p>
   * <B>Note:</B> If a property is specified as part of the {@code url} and
   * is also specified in the {@code Properties} object, it is
   * implementation-defined as to which value will take precedence.
   * For maximum portability, an application should only specify a
   * property once.
   *
   * @return a Connection to the URL
   * @throws SQLException if a database access error occurs or the url is
   * {@code null}
   * @throws SQLTimeoutException when the driver has determined that the
   * timeout value specified by the {@code setLoginTimeout} method
   * has been exceeded and has at least tried to cancel the
   * current database connection attempt
   */
  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(url, properties);
  }

  @Override
  public String toString() {
    return String.format("Connections from url: %s, and properties: %s", url, properties);
  }

}
