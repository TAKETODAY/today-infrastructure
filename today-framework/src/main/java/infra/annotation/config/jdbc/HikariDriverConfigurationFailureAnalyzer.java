/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config.jdbc;

import infra.jdbc.CannotGetJdbcConnectionException;
import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of a Hikari configuration
 * failure caused by the use of the unsupported 'dataSourceClassName' property.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HikariDriverConfigurationFailureAnalyzer
        extends AbstractFailureAnalyzer<CannotGetJdbcConnectionException> {

  private static final String EXPECTED_MESSAGE = "cannot use driverClassName and dataSourceClassName together.";

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, CannotGetJdbcConnectionException cause) {
    Throwable subCause = cause.getCause();
    if (subCause == null || !EXPECTED_MESSAGE.equals(subCause.getMessage())) {
      return null;
    }
    return new FailureAnalysis(
            "Configuration of the Hikari connection pool failed: 'dataSourceClassName' is not supported.",
            "Infra auto-configures only a driver and can't specify a custom "
                    + "DataSource. Consider configuring the Hikari DataSource in your own configuration.",
            cause);
  }

}