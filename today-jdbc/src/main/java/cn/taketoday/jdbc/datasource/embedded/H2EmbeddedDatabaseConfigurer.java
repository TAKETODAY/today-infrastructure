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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an H2 embedded database instance.
 *
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class H2EmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

  @Nullable
  private static H2EmbeddedDatabaseConfigurer instance;

  private final Class<? extends Driver> driverClass;

  /**
   * Get the singleton {@code H2EmbeddedDatabaseConfigurer} instance.
   *
   * @return the configurer instance
   * @throws ClassNotFoundException if H2 is not on the classpath
   */
  public static synchronized H2EmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
    if (instance == null) {
      instance = new H2EmbeddedDatabaseConfigurer(
              ClassUtils.forName("org.h2.Driver", H2EmbeddedDatabaseConfigurer.class.getClassLoader()));
    }
    return instance;
  }

  private H2EmbeddedDatabaseConfigurer(Class<? extends Driver> driverClass) {
    this.driverClass = driverClass;
  }

  @Override
  public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
    properties.setDriverClass(this.driverClass);
    properties.setUrl(String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false", databaseName));
    properties.setUsername("sa");
    properties.setPassword("");
  }

}
