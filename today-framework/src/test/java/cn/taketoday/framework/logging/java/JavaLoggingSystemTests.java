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

package cn.taketoday.framework.logging.java;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileFilter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.taketoday.framework.logging.AbstractLoggingSystemTests;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.framework.logging.LoggerConfiguration;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.framework.logging.LoggingSystemProperty;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JavaLoggingSystem}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Ben Hale
 */
@ExtendWith(OutputCaptureExtension.class)
class JavaLoggingSystemTests extends AbstractLoggingSystemTests {

  private static final FileFilter LOG_FILTER = pathname -> pathname.getName().startsWith("infra-app.log");

  private final JavaLoggingSystem loggingSystem = new JavaLoggingSystem(getClass().getClassLoader());

  private Logger logger;

  private Locale defaultLocale;

  @BeforeEach
  void init() {
    this.logger = Logger.getLogger(getClass().getName());
    this.defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterEach
  void resetLogger() {
    this.logger.setLevel(Level.OFF);
    this.loggingSystem.getShutdownHandler().run();
  }

  @AfterEach
  void restoreLocale() {
    Locale.setDefault(this.defaultLocale);
  }

  @Test
  void noFile(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.logger.info("Hidden");
    this.loggingSystem.initialize(null, null, null);
    this.logger.info("Hello world");
    assertThat(output).contains("Hello world").doesNotContain("Hidden");
    assertThat(new File(tmpDir(), "infra-app.log")).doesNotExist();
  }

  @Test
  void withFile(CapturedOutput output) {
    File temp = new File(tmpDir());
    File[] logFiles = temp.listFiles(LOG_FILTER);
    for (File file : logFiles) {
      file.delete();
    }
    this.loggingSystem.beforeInitialize();
    this.logger.info("Hidden");
    this.loggingSystem.initialize(null, null, getLogFile(null, tmpDir()));
    this.logger.info("Hello world");
    assertThat(output).contains("Hello world").doesNotContain("Hidden");
    assertThat(temp.listFiles(LOG_FILTER)).isNotEmpty();
  }

  @Test
  void testCustomFormatter(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null, null, null);
    this.logger.info("Hello world");
    assertThat(output).contains("Hello world").contains("???? INFO [");
  }

  @Test
  void testSystemPropertyInitializesFormat(CapturedOutput output) {
    System.setProperty(LoggingSystemProperty.PID.getEnvironmentVariableName(), "1234");
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null,
            "classpath:" + ClassUtils.addResourcePathToPackagePath(getClass(), "logging.properties"), null);
    this.logger.info("Hello world");
    this.logger.info("Hello world");
    assertThat(output).contains("Hello world").contains("1234 INFO [");
  }

  @Test
  void testNonDefaultConfigLocation(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null,
            "classpath:logging-nondefault.properties", null);
    this.logger.info("Hello world");
    assertThat(output).contains("INFO: Hello");
  }

  @Test
  void testNonexistentConfigLocation() {
    this.loggingSystem.beforeInitialize();
    assertThatIllegalStateException()
            .isThrownBy(() -> this.loggingSystem.initialize(
                    null, "classpath:logging-nonexistent.properties", null));
  }

  @Test
  void getSupportedLevels() {
    assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(
            EnumSet.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF));
  }

  @Test
  void setLevel(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null, null, null);
    this.logger.fine("Hello");
    this.loggingSystem.setLogLevel("cn.taketoday.framework", LogLevel.DEBUG);
    this.logger.fine("Hello");
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
  }

  @Test
  void setLevelToNull(CapturedOutput output) {
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null, null, null);
    this.logger.fine("Hello");
    this.loggingSystem.setLogLevel("cn.taketoday.framework", LogLevel.DEBUG);
    this.logger.fine("Hello");
    this.loggingSystem.setLogLevel("cn.taketoday.framework", null);
    this.logger.fine("Hello");
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
  }

  @Test
  void getLoggingConfigurations() {
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null, null, null);
    this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
    List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
    assertThat(configurations).isNotEmpty();
    assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
  }

  @Test
  void getLoggingConfiguration() {
    this.loggingSystem.beforeInitialize();
    this.loggingSystem.initialize(null, null, null);
    this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
    LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
    assertThat(configuration)
            .isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.DEBUG, LogLevel.DEBUG));
  }

}
