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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;

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
    assertThat(properties.getProperty(LoggingSystemProperties.LOG_FILE)).isEqualTo("log.file");
    assertThat(properties.getProperty(LoggingSystemProperties.LOG_PATH)).isNull();
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
    assertThat(properties.getProperty(LoggingSystemProperties.LOG_FILE))
            .isEqualTo("logpath" + File.separatorChar + "infra-app.log");
    assertThat(properties.getProperty(LoggingSystemProperties.LOG_PATH)).isEqualTo("logpath");
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
    assertThat(properties.getProperty(LoggingSystemProperties.LOG_FILE)).isEqualTo("log.file");
    assertThat(properties.getProperty(LoggingSystemProperties.LOG_PATH)).isEqualTo("logpath");
  }

  private PropertyResolver getPropertyResolver(Map<String, Object> properties) {
    PropertySource<?> propertySource = new MapPropertySource("properties", properties);
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(propertySource);
    return new PropertySourcesPropertyResolver(propertySources);
  }

}
