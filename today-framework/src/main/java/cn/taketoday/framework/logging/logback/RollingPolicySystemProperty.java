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

package cn.taketoday.framework.logging.logback;

/**
 * Logback rolling policy system properties that can later be used by log configuration
 * files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LogbackLoggingSystemProperties
 * @since 4.0
 */
public enum RollingPolicySystemProperty {

  /**
   * Logging system property for the rolled-over log file name pattern.
   */
  FILE_NAME_PATTERN("file-name-pattern", "logging.pattern.rolling-file-name"),

  /**
   * Logging system property for the clean history on start flag.
   */
  CLEAN_HISTORY_ON_START("clean-history-on-start", "logging.file.clean-history-on-start"),

  /**
   * Logging system property for the file log max size.
   */
  MAX_FILE_SIZE("max-file-size", "logging.file.max-size"),

  /**
   * Logging system property for the file total size cap.
   */
  TOTAL_SIZE_CAP("total-size-cap", "logging.file.total-size-cap"),

  /**
   * Logging system property for the file log max history.
   */
  MAX_HISTORY("max-history", "logging.file.max-history");

  private final String environmentVariableName;

  private final String applicationPropertyName;

  private final String deprecatedApplicationPropertyName;

  RollingPolicySystemProperty(String applicationPropertyName, String deprecatedApplicationPropertyName) {
    this.environmentVariableName = "LOGBACK_ROLLINGPOLICY_" + name();
    this.applicationPropertyName = "logging.logback.rollingpolicy." + applicationPropertyName;
    this.deprecatedApplicationPropertyName = deprecatedApplicationPropertyName;
  }

  /**
   * Return the name of environment variable that can be used to access this property.
   *
   * @return the environment variable name
   */
  public String getEnvironmentVariableName() {
    return this.environmentVariableName;
  }

  String getApplicationPropertyName() {
    return this.applicationPropertyName;
  }

  String getDeprecatedApplicationPropertyName() {
    return this.deprecatedApplicationPropertyName;
  }

}
