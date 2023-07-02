/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.logging;

import cn.taketoday.lang.Nullable;

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
  APPLICATION_NAME("LOGGED_APPLICATION_NAME"),

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

  @Nullable
  private final String applicationPropertyName;

  LoggingSystemProperty(String environmentVariableName) {
    this(environmentVariableName, null);
  }

  LoggingSystemProperty(String environmentVariableName, @Nullable String applicationPropertyName) {
    this.environmentVariableName = environmentVariableName;
    this.applicationPropertyName = applicationPropertyName;
  }

  /**
   * Return the name of environment variable that can be used to access this property.
   *
   * @return the environment variable name
   */
  public String getEnvironmentVariableName() {
    return this.environmentVariableName;
  }

  @Nullable
  String getApplicationPropertyName() {
    return this.applicationPropertyName;
  }

}
