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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingSystemProperties}.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 */
class LoggingSystemPropertiesTests {

  private Set<Object> systemPropertyNames;

  @BeforeEach
  void captureSystemPropertyNames() {
    for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
      System.getProperties().remove(property.getEnvironmentVariableName());
    }
    this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
  }

  @AfterEach
  void restoreSystemProperties() {
    System.getProperties().keySet().retainAll(this.systemPropertyNames);
  }

  @Test
  void pidIsSet() {
    new LoggingSystemProperties(new MockEnvironment()).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.PID)).isNotNull();
  }

  @Test
  void consoleLogPatternIsSet() {
    new LoggingSystemProperties(new MockEnvironment().withProperty("logging.pattern.console", "console pattern"))
            .apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN)).isEqualTo("console pattern");
  }

  @Test
  void consoleCharsetWhenNoPropertyUsesUtf8() {
    new LoggingSystemProperties(new MockEnvironment()).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET)).isEqualTo("UTF-8");
  }

  @Test
  void consoleCharsetIsSet() {
    new LoggingSystemProperties(new MockEnvironment().withProperty("logging.charset.console", "UTF-16"))
            .apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET)).isEqualTo("UTF-16");
  }

  @Test
  void fileLogPatternIsSet() {
    new LoggingSystemProperties(new MockEnvironment().withProperty("logging.pattern.file", "file pattern"))
            .apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.FILE_PATTERN)).isEqualTo("file pattern");
  }

  @Test
  void fileCharsetWhenNoPropertyUsesUtf8() {
    new LoggingSystemProperties(new MockEnvironment()).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.FILE_CHARSET)).isEqualTo("UTF-8");
  }

  @Test
  void fileCharsetIsSet() {
    new LoggingSystemProperties(new MockEnvironment().withProperty("logging.charset.file", "UTF-16")).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.FILE_CHARSET)).isEqualTo("UTF-16");
  }

  @Test
  void consoleLogPatternCanReferencePid() {
    new LoggingSystemProperties(environment("logging.pattern.console", "${PID:unknown}")).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN)).matches("[0-9]+");
  }

  @Test
  void fileLogPatternCanReferencePid() {
    new LoggingSystemProperties(environment("logging.pattern.file", "${PID:unknown}")).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.FILE_PATTERN)).matches("[0-9]+");
  }

  private String getSystemProperty(LoggingSystemProperty property) {
    return System.getProperty(property.getEnvironmentVariableName());
  }

  @Test
  void correlationPatternIsSet() {
    new LoggingSystemProperties(
            new MockEnvironment().withProperty("logging.pattern.correlation", "correlation pattern"))
            .apply(null);
    assertThat(System.getProperty(LoggingSystemProperty.CORRELATION_PATTERN.getEnvironmentVariableName()))
            .isEqualTo("correlation pattern");
  }

  @Test
  void defaultValueResolverIsUsed() {
    MockEnvironment environment = new MockEnvironment();
    Map<String, String> defaultValues = Map
            .of(LoggingSystemProperty.CORRELATION_PATTERN.getApplicationPropertyName(), "default correlation pattern");
    new LoggingSystemProperties(environment, defaultValues::get, null).apply(null);
    assertThat(System.getProperty(LoggingSystemProperty.CORRELATION_PATTERN.getEnvironmentVariableName()))
            .isEqualTo("default correlation pattern");
  }

  @Test
  void loggedApplicationNameWhenHasApplicationName() {
    new LoggingSystemProperties(new MockEnvironment().withProperty("app.name", "test")).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.APPLICATION_NAME)).isEqualTo("[test] ");
  }

  @Test
  void loggedApplicationNameWhenHasNoApplicationName() {
    new LoggingSystemProperties(new MockEnvironment()).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.APPLICATION_NAME)).isNull();
  }

  @Test
  void loggedApplicationNameWhenApplicationNameLoggingDisabled() {
    new LoggingSystemProperties(new MockEnvironment().withProperty("app.name", "test")
            .withProperty("logging.include-application-name", "false")).apply(null);
    assertThat(getSystemProperty(LoggingSystemProperty.APPLICATION_NAME)).isNull();
  }

  private Environment environment(String key, Object value) {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addLast(new MapPropertySource("test", Collections.singletonMap(key, value)));
    return environment;
  }

}
