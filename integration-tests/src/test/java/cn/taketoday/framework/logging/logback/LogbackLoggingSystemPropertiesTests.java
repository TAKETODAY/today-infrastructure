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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.framework.logging.LoggingSystemProperties;
import cn.taketoday.framework.logging.LoggingSystemProperty;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackLoggingSystemProperties}.
 *
 * @author Phillip Webb
 */
class LogbackLoggingSystemPropertiesTests {

  private Set<Object> systemPropertyNames;

  private MockEnvironment environment;

  @BeforeEach
  void captureSystemPropertyNames() {
    for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
      System.getProperties().remove(property.getEnvironmentVariableName());
    }
    this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
    this.environment = new MockEnvironment();
    this.environment.setConversionService(ApplicationConversionService.getSharedInstance());

  }

  @AfterEach
  void restoreSystemProperties() {
    System.getProperties().keySet().retainAll(this.systemPropertyNames);
  }

  @Test
  void applySetsStandardSystemProperties() {
    this.environment.setProperty("logging.pattern.console", "boot");
    new LogbackLoggingSystemProperties(this.environment).apply();
    assertThat(System.getProperties())
            .containsEntry(LoggingSystemProperty.CONSOLE_PATTERN.getEnvironmentVariableName(), "boot");
  }

  @Test
  void applySetsLogbackSystemProperties() {
    this.environment.setProperty("logging.logback.rollingpolicy.file-name-pattern", "fnp");
    this.environment.setProperty("logging.logback.rollingpolicy.clean-history-on-start", "chos");
    this.environment.setProperty("logging.logback.rollingpolicy.max-file-size", "1KB");
    this.environment.setProperty("logging.logback.rollingpolicy.total-size-cap", "2KB");
    this.environment.setProperty("logging.logback.rollingpolicy.max-history", "mh");
    new LogbackLoggingSystemProperties(this.environment).apply();
    assertThat(System.getProperties())
            .containsEntry(RollingPolicySystemProperty.FILE_NAME_PATTERN.getEnvironmentVariableName(), "fnp")
            .containsEntry(RollingPolicySystemProperty.CLEAN_HISTORY_ON_START.getEnvironmentVariableName(), "chos")
            .containsEntry(RollingPolicySystemProperty.MAX_FILE_SIZE.getEnvironmentVariableName(), "1024")
            .containsEntry(RollingPolicySystemProperty.TOTAL_SIZE_CAP.getEnvironmentVariableName(), "2048")
            .containsEntry(RollingPolicySystemProperty.MAX_HISTORY.getEnvironmentVariableName(), "mh");
  }

  @Test
  void applySetsLogbackSystemPropertiesFromDeprecated() {
    this.environment.setProperty("logging.pattern.rolling-file-name", "fnp");
    this.environment.setProperty("logging.file.clean-history-on-start", "chos");
    this.environment.setProperty("logging.file.max-size", "1KB");
    this.environment.setProperty("logging.file.total-size-cap", "2KB");
    this.environment.setProperty("logging.file.max-history", "mh");
    new LogbackLoggingSystemProperties(this.environment).apply();
    assertThat(System.getProperties())
            .containsEntry(RollingPolicySystemProperty.FILE_NAME_PATTERN.getEnvironmentVariableName(), "fnp")
            .containsEntry(RollingPolicySystemProperty.CLEAN_HISTORY_ON_START.getEnvironmentVariableName(), "chos")
            .containsEntry(RollingPolicySystemProperty.MAX_FILE_SIZE.getEnvironmentVariableName(), "1024")
            .containsEntry(RollingPolicySystemProperty.TOTAL_SIZE_CAP.getEnvironmentVariableName(), "2048")
            .containsEntry(RollingPolicySystemProperty.MAX_HISTORY.getEnvironmentVariableName(), "mh");
  }

  @Test
  void consoleCharsetWhenNoPropertyUsesDefault() {
    new LoggingSystemProperties(new MockEnvironment()).apply(null);
    assertThat(System.getProperty(LoggingSystemProperty.CONSOLE_CHARSET.getEnvironmentVariableName()))
            .isEqualTo(Charset.defaultCharset().name());
  }

  @Test
  void fileCharsetWhenNoPropertyUsesDefault() {
    new LoggingSystemProperties(new MockEnvironment()).apply(null);
    assertThat(System.getProperty(LoggingSystemProperty.FILE_CHARSET.getEnvironmentVariableName()))
            .isEqualTo(Charset.defaultCharset().name());
  }

}
