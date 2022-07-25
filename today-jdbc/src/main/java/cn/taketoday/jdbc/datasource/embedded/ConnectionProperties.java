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

import java.sql.Driver;

/**
 * {@code ConnectionProperties} serves as a simple data container that allows
 * essential JDBC connection properties to be configured consistently,
 * independent of the actual {@link javax.sql.DataSource DataSource}
 * implementation.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @see DataSourceFactory
 * @since 4.0
 */
public interface ConnectionProperties {

  /**
   * Set the JDBC driver class to use to connect to the database.
   *
   * @param driverClass the jdbc driver class
   */
  void setDriverClass(Class<? extends Driver> driverClass);

  /**
   * Set the JDBC connection URL for the database.
   *
   * @param url the connection url
   */
  void setUrl(String url);

  /**
   * Set the username to use to connect to the database.
   *
   * @param username the username
   */
  void setUsername(String username);

  /**
   * Set the password to use to connect to the database.
   *
   * @param password the password
   */
  void setPassword(String password);

}
