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

package infra.app.logging;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import infra.core.env.MapPropertySource;
import infra.core.env.PropertyResolver;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.env.PropertySourcesPropertyResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogFile}.
 *
 * @author Phillip Webb
 */
class LogFileTests {

  @Test
  void noProperties() {
    PropertyResolver resolver = getPropertyResolver(Collections.emptyMap());
    LogFile logFile = LogFile.get(resolver);
    assertThat(logFile).isNull();
  }

  @Test
  void loggingFile() {
    PropertyResolver resolver = getPropertyResolver(Collections.singletonMap("logging.file.name", "log.file"));
    testLoggingFile(resolver);
  }

  private void testLoggingFile(PropertyResolver resolver) {
    LogFile logFile = LogFile.get(resolver);
    Properties properties = new Properties();
    logFile.applyTo(properties);
    assertThat(logFile).hasToString("log.file");
    assertThat(properties.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
            .isEqualTo("log.file");
    assertThat(properties.getProperty(LoggingSystemProperty.LOG_PATH.getEnvironmentVariableName())).isNull();
  }

  @Test
  void loggingPath() {
    PropertyResolver resolver = getPropertyResolver(Collections.singletonMap("logging.file.path", "logpath"));
    testLoggingPath(resolver);
  }

  private void testLoggingPath(PropertyResolver resolver) {
    LogFile logFile = LogFile.get(resolver);
    Properties properties = new Properties();
    logFile.applyTo(properties);
    assertThat(logFile).hasToString("logpath" + File.separatorChar + "infra-app.log");
    assertThat(properties.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
            .isEqualTo("logpath" + File.separatorChar + "infra-app.log");
    assertThat(properties.getProperty(LoggingSystemProperty.LOG_PATH.getEnvironmentVariableName()))
            .isEqualTo("logpath");
  }

  @Test
  void loggingFileAndPath() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("logging.file.name", "log.file");
    properties.put("logging.file.path", "logpath");
    PropertyResolver resolver = getPropertyResolver(properties);
    testLoggingFileAndPath(resolver);
  }

  private void testLoggingFileAndPath(PropertyResolver resolver) {
    LogFile logFile = LogFile.get(resolver);
    Properties properties = new Properties();
    logFile.applyTo(properties);
    assertThat(logFile).hasToString("log.file");
    assertThat(properties.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
            .isEqualTo("log.file");
    assertThat(properties.getProperty(LoggingSystemProperty.LOG_PATH.getEnvironmentVariableName()))
            .isEqualTo("logpath");
  }

  private PropertyResolver getPropertyResolver(Map<String, Object> properties) {
    PropertySource<?> propertySource = new MapPropertySource("properties", properties);
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(propertySource);
    return new PropertySourcesPropertyResolver(propertySources);
  }

}
