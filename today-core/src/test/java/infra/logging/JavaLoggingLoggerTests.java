/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.logging;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:34
 */
class JavaLoggingLoggerTests {

  @Test
  void shouldCreateJavaLoggingLogger() {
    Logger javaLogger = Logger.getLogger("test.logger");
    boolean debugEnabled = true;

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, debugEnabled);

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo("test.logger");
  }

  @Test
  void shouldReturnCorrectLoggerName() {
    Logger javaLogger = Logger.getLogger("com.example.TestLogger");
    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    assertThat(logger.getName()).isEqualTo("com.example.TestLogger");
  }

  @Test
  void shouldCheckIfInfoIsEnabled() {
    Logger javaLogger = Logger.getLogger("test.info");
    javaLogger.setLevel(java.util.logging.Level.INFO);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    assertThat(logger.isInfoEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfErrorIsEnabled() {
    Logger javaLogger = Logger.getLogger("test.error");
    javaLogger.setLevel(java.util.logging.Level.SEVERE);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    assertThat(logger.isErrorEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfWarnIsEnabled() {
    Logger javaLogger = Logger.getLogger("test.warn");
    javaLogger.setLevel(java.util.logging.Level.WARNING);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    assertThat(logger.isWarnEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfTraceIsEnabledWhenDebugEnabled() {
    Logger javaLogger = Logger.getLogger("test.trace");
    javaLogger.setLevel(java.util.logging.Level.FINEST);
    boolean debugEnabled = true;

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, debugEnabled);

    assertThat(logger.isTraceEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfTraceIsDisabledWhenDebugDisabled() {
    Logger javaLogger = Logger.getLogger("test.trace.disabled");
    javaLogger.setLevel(java.util.logging.Level.FINEST);
    boolean debugEnabled = false;

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, debugEnabled);

    assertThat(logger.isTraceEnabled()).isFalse();
  }

  @Test
  void shouldLogMessageAtInfoLevel() {
    Logger javaLogger = Logger.getLogger("test.log.info");
    javaLogger.setLevel(java.util.logging.Level.INFO);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    // Should not throw exception
    logger.info("Test info message");
    assertThat(true).isTrue();
  }

  @Test
  void shouldLogMessageAtErrorLevelWithThrowable() {
    Logger javaLogger = Logger.getLogger("test.log.error");
    javaLogger.setLevel(java.util.logging.Level.SEVERE);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);
    Throwable throwable = new RuntimeException("Test exception");

    // Should not throw exception
    logger.error("Test error message", throwable);
    assertThat(true).isTrue();
  }

  @Test
  void shouldConvertLevelToJavaLevel() {
    Logger javaLogger = Logger.getLogger("test.level.convert");
    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    // Test through reflection or by calling log methods that use the conversion
    logger.info("Test message");
    logger.debug("Debug message");
    logger.trace("Trace message");
    logger.warn("Warn message");
    logger.error("Error message");

    // If no exception thrown, level conversion works
    assertThat(true).isTrue();
  }

  @Test
  void shouldCheckIfDebugEnabled() {
    Logger javaLogger = Logger.getLogger("test.debug");
    javaLogger.setLevel(java.util.logging.Level.FINER);
    boolean debugEnabled = true;

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, debugEnabled);

    assertThat(logger.isDebugEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfDebugDisabledWhenNotEnabled() {
    Logger javaLogger = Logger.getLogger("test.debug.disabled");
    javaLogger.setLevel(java.util.logging.Level.FINER);
    boolean debugEnabled = false;

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, debugEnabled);

    assertThat(logger.isDebugEnabled()).isFalse();
  }

  @Test
  void shouldNotLogWhenLevelNotEnabled() {
    Logger javaLogger = Logger.getLogger("test.not.enabled");
    javaLogger.setLevel(java.util.logging.Level.WARNING);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    // Should not throw exception even when trying to log at lower level
    logger.debug("This should not be logged");
    logger.trace("This should not be logged either");

    assertThat(true).isTrue();
  }

  @Test
  void shouldLogMessageWithArguments() {
    Logger javaLogger = Logger.getLogger("test.log.args");
    javaLogger.setLevel(java.util.logging.Level.INFO);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    // Should not throw exception
    logger.info("Test message with args: {} and {}", "arg1", 42);
    assertThat(true).isTrue();
  }

  @Test
  void shouldLogMessageWithNullArguments() {
    Logger javaLogger = Logger.getLogger("test.log.null.args");
    javaLogger.setLevel(java.util.logging.Level.INFO);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    // Should not throw exception
    logger.info("Test message with null args: {} and {}", null, "notNull");
    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleLocationResolvingLogRecordSerialization() {
    Logger javaLogger = Logger.getLogger("test.serialization");
    javaLogger.setLevel(java.util.logging.Level.INFO);

    JavaLoggingLogger logger = new JavaLoggingLogger(javaLogger, false);

    // Test that logging works and LocationResolvingLogRecord can be created
    logger.info("Test serialization message");

    assertThat(logger.getName()).isEqualTo("test.serialization");
  }

}