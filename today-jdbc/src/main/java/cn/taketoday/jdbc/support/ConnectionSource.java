/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.jdbc.JdbcConnection;

/**
 * An abstraction layer for providing jdbc connection to use from
 * {@link JdbcConnection}
 */
public interface ConnectionSource {

  /**
   * get jdbc connection
   */
  Connection getConnection() throws SQLException;

  /**
   * A ConnectionSource that will wrap externally managed connection with proxy
   * that will omit {@link Connection#close()} or {@link Connection#commit()}
   * calls. This is useful to make {@link JdbcConnection} work with
   * externally managed transactions
   *
   * @param connection connection to wrap
   * @return a connection wrapper that represent a nested connection
   */
  static ConnectionSource join(final Connection connection) {
    final class NestedConnectionSource implements ConnectionSource {
      @Override
      public Connection getConnection() throws SQLException {
        return new NestedConnection(connection);
      }
    }
    return new NestedConnectionSource();
  }

  /**
   * create a ConnectionSource from data-source
   *
   * @see DataSourceConnectionSource
   */
  static ConnectionSource fromDataSource(DataSource source) {
    return new DataSourceConnectionSource(source);
  }

  /**
   * Attempts to establish a connection when invoke getConnection()
   * to the given database URL. The <code>DriverManager</code> attempts
   * to select an appropriate driver from the set of registered JDBC drivers.
   * <p>
   * <B>Note:</B> If a property is specified as part of the {@code url} and
   * is also specified in the {@code Properties} object, it is
   * implementation-defined as to which value will take precedence.
   * For maximum portability, an application should only specify a
   * property once.
   *
   * @param url a database url of the form
   * <code> jdbc:<em>subprotocol</em>:<em>subname</em></code>
   * @param info a list of arbitrary string tag/value pairs as
   * connection arguments; normally at least a "user" and
   * "password" property should be included
   * @return GenericConnectionSource
   * @see java.sql.DriverManager#getConnection(String, Properties)
   */
  static ConnectionSource from(String url, Properties info) {
    return new GenericConnectionSource(url, info);
  }

  /**
   * Attempts to establish a connection when invoke getConnection()
   * to the given database URL. The <code>DriverManager</code> attempts
   * to select an appropriate driver from the set of registered JDBC drivers.
   * <p>
   * <B>Note:</B> If a property is specified as part of the {@code url} and
   * is also specified in the {@code Properties} object, it is
   * implementation-defined as to which value will take precedence.
   * For maximum portability, an application should only specify a
   * property once.
   *
   * @param url JDBC database url
   * @param user database username
   * @param password database password
   * @return GenericConnectionSource
   * @see java.sql.DriverManager#getConnection(String, Properties)
   */
  static ConnectionSource from(String url, String user, String password) {
    return new GenericConnectionSource(url, user, password);
  }

}
