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

package infra.app.logging;

import org.jspecify.annotations.Nullable;

/**
 * Logging system properties that can later be used by log configuration files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LoggingSystemProperties
 * @since 4.0
 */
public enum LoggingSystemProperty {

  /**
   * Logging system property for the application name that should be logged.
   */
  APPLICATION_NAME("APPLICATION_NAME", "app.name", "logging.include-application-name"),

  /**
   * Logging system property for the application group that should be logged.
   */
  APPLICATION_GROUP("APPLICATION_GROUP", "app.group", "logging.include-application-group"),

  /**
   * Logging system property for the process ID.
   */
  PID("PID"),

  /**
   * Logging system property for the log file.
   */
  LOG_FILE("LOG_FILE"),

  /**
   * Logging system property for the log path.
   */
  LOG_PATH("LOG_PATH"),

  /**
   * Logging system property for the console log charset.
   */
  CONSOLE_CHARSET("CONSOLE_LOG_CHARSET", "logging.charset.console"),

  /**
   * Logging system property for the file log charset.
   */
  FILE_CHARSET("FILE_LOG_CHARSET", "logging.charset.file"),

  /**
   * Logging system property for the console log.
   */
  CONSOLE_THRESHOLD("CONSOLE_LOG_THRESHOLD", "logging.threshold.console"),

  /**
   * Logging system property for the file log.
   */
  FILE_THRESHOLD("FILE_LOG_THRESHOLD", "logging.threshold.file"),

  /**
   * Logging system property for the exception conversion word.
   */
  EXCEPTION_CONVERSION_WORD("LOG_EXCEPTION_CONVERSION_WORD", "logging.exception-conversion-word"),

  /**
   * Logging system property for the console log pattern.
   */
  CONSOLE_PATTERN("CONSOLE_LOG_PATTERN", "logging.pattern.console"),

  /**
   * Logging system property for the file log pattern.
   */
  FILE_PATTERN("FILE_LOG_PATTERN", "logging.pattern.file"),

  /**
   * Logging system property for the console structured logging format.
   *
   * @since 5.0
   */
  CONSOLE_STRUCTURED_FORMAT("CONSOLE_LOG_STRUCTURED_FORMAT", "logging.structured.format.console"),

  /**
   * Logging system property for the file structured logging format.
   *
   * @since 5.0
   */
  FILE_STRUCTURED_FORMAT("FILE_LOG_STRUCTURED_FORMAT", "logging.structured.format.file"),

  /**
   * Logging system property for the log level pattern.
   */
  LEVEL_PATTERN("LOG_LEVEL_PATTERN", "logging.pattern.level"),

  /**
   * Logging system property for the date-format pattern.
   */
  DATEFORMAT_PATTERN("LOG_DATEFORMAT_PATTERN", "logging.pattern.dateformat"),

  /**
   * Logging system property for the correlation pattern.
   */
  CORRELATION_PATTERN("LOG_CORRELATION_PATTERN", "logging.pattern.correlation");

  private final String environmentVariableName;

  /**
   * Return the name of the application property name that can be used to set this
   * property.
   */
  @Nullable
  public final String applicationPropertyName;

  @Nullable
  final String includePropertyName;

  LoggingSystemProperty(String environmentVariableName) {
    this(environmentVariableName, null);
  }

  LoggingSystemProperty(String environmentVariableName, @Nullable String applicationPropertyName) {
    this(environmentVariableName, applicationPropertyName, null);
  }

  LoggingSystemProperty(String environmentVariableName, @Nullable String applicationPropertyName, @Nullable String includePropertyName) {
    this.environmentVariableName = environmentVariableName;
    this.applicationPropertyName = applicationPropertyName;
    this.includePropertyName = includePropertyName;
  }

  /**
   * Return the name of environment variable that can be used to access this property.
   *
   * @return the environment variable name
   */
  public String getEnvironmentVariableName() {
    return this.environmentVariableName;
  }

}
