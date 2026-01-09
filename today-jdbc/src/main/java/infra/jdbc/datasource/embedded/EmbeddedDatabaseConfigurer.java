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

package infra.jdbc.datasource.embedded;

import java.util.function.UnaryOperator;

import javax.sql.DataSource;

import infra.lang.Assert;

/**
 * {@code EmbeddedDatabaseConfigurer} encapsulates the configuration required to
 * create, connect to, and shut down a specific type of embedded database such as
 * HSQL, H2, or Derby.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface EmbeddedDatabaseConfigurer {

  /**
   * Configure the properties required to create and connect to the embedded database.
   *
   * @param properties connection properties to configure
   * @param databaseName the name of the embedded database
   */
  void configureConnectionProperties(ConnectionProperties properties, String databaseName);

  /**
   * Shut down the embedded database instance that backs the supplied {@link DataSource}.
   *
   * @param dataSource the corresponding {@link DataSource}
   * @param databaseName the name of the database being shut down
   */
  void shutdown(DataSource dataSource, String databaseName);

  /**
   * Return a configurer instance for the given embedded database type.
   *
   * @param type the embedded database type (HSQL, H2 or Derby)
   * @return the configurer instance
   * @throws IllegalStateException if the driver for the specified database type is not available
   */
  static EmbeddedDatabaseConfigurer from(EmbeddedDatabaseType type) throws IllegalStateException {
    Assert.notNull(type, "EmbeddedDatabaseType is required");
    try {
      return switch (type) {
        case HSQL -> HsqlEmbeddedDatabaseConfigurer.getInstance();
        case H2 -> H2EmbeddedDatabaseConfigurer.getInstance();
        case DERBY -> DerbyEmbeddedDatabaseConfigurer.getInstance();
      };
    }
    catch (ClassNotFoundException | NoClassDefFoundError ex) {
      throw new IllegalStateException("Driver for test database type [" + type + "] is not available", ex);
    }
  }

  /**
   * Customize the default configurer for the given embedded database type.
   * <p>The {@code customizer} typically uses
   * {@link EmbeddedDatabaseConfigurerDelegate} to customize things as necessary.
   *
   * @param type the {@linkplain EmbeddedDatabaseType embedded database type}
   * @param customizer the customizer to return based on the default
   * @return the customized configurer instance
   * @throws IllegalStateException if the driver for the specified database type is not available
   */
  static EmbeddedDatabaseConfigurer customizeConfigurer(
          EmbeddedDatabaseType type, UnaryOperator<EmbeddedDatabaseConfigurer> customizer) {

    EmbeddedDatabaseConfigurer defaultConfigurer = from(type);
    return customizer.apply(defaultConfigurer);
  }

}
