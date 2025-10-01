/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.datasource.embedded;

import org.jspecify.annotations.Nullable;

import java.sql.Driver;

import infra.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an HSQL embedded database instance.
 *
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 4.0
 */
final class HsqlEmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

  @Nullable
  private static HsqlEmbeddedDatabaseConfigurer instance;

  private final Class<? extends Driver> driverClass;

  /**
   * Get the singleton {@link HsqlEmbeddedDatabaseConfigurer} instance.
   *
   * @return the configurer instance
   * @throws ClassNotFoundException if HSQL is not on the classpath
   */
  public static synchronized HsqlEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
    if (instance == null) {
      instance = new HsqlEmbeddedDatabaseConfigurer(
              ClassUtils.forName("org.hsqldb.jdbcDriver", HsqlEmbeddedDatabaseConfigurer.class.getClassLoader()));
    }
    return instance;
  }

  private HsqlEmbeddedDatabaseConfigurer(Class<? extends Driver> driverClass) {
    this.driverClass = driverClass;
  }

  @Override
  public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
    properties.setDriverClass(this.driverClass);
    properties.setUrl("jdbc:hsqldb:mem:" + databaseName);
    properties.setUsername("sa");
    properties.setPassword("");
  }

}
