/*
 * Copyright 2012-present the original author or authors.
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

package infra.flyway.config;

import org.jspecify.annotations.Nullable;

import infra.jdbc.config.DatabaseDriver;

/**
 * Details required for Flyway to establish a connection to an SQL service using JDBC.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface FlywayConnectionDetails {

  /**
   * Username for the database or {@code null} if no Flyway-specific configuration is
   * required.
   *
   * @return the username for the database or {@code null}
   */
  @Nullable String getUsername();

  /**
   * Password for the database or {@code null} if no Flyway-specific configuration is
   * required.
   *
   * @return the password for the database or {@code null}
   */
  @Nullable String getPassword();

  /**
   * JDBC URL for the database or {@code null} if no Flyway-specific configuration is
   * required.
   *
   * @return the JDBC URL for the database or {@code null}
   */
  @Nullable String getJdbcUrl();

  /**
   * The name of the JDBC driver class. Defaults to the class name of the driver
   * specified in the JDBC URL or {@code null} when no JDBC URL is configured.
   *
   * @return the JDBC driver class name or {@code null}
   * @see #getJdbcUrl()
   * @see DatabaseDriver#fromJdbcUrl(String)
   * @see DatabaseDriver#getDriverClassName()
   */
  default @Nullable String getDriverClassName() {
    String jdbcUrl = getJdbcUrl();
    return (jdbcUrl != null) ? DatabaseDriver.fromJdbcUrl(jdbcUrl).getDriverClassName() : null;
  }

}
