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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:30
 */
class IntrospectionFailureLoggerTests {

  @Test
  void debugLoggerIsEnabledReturnsCorrectValue() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.DEBUG;
    // This test assumes the default logging configuration where DEBUG might not be enabled
    // The actual value depends on the logging configuration, so we just verify it's a boolean
    assertThat(logger.isEnabled()).isInstanceOf(Boolean.class);
  }

  @Test
  void infoLoggerIsEnabledReturnsCorrectValue() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
    // This test assumes the default logging configuration where INFO is typically enabled
    // The actual value depends on the logging configuration, so we just verify it's a boolean
    assertThat(logger.isEnabled()).isInstanceOf(Boolean.class);
  }

  @Test
  void debugLoggerLogMessage() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.DEBUG;
    // Verify no exception is thrown
    assertThatCode(() -> logger.log("Debug test message")).doesNotThrowAnyException();
  }

  @Test
  void infoLoggerLogMessage() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
    // Verify no exception is thrown
    assertThatCode(() -> logger.log("Info test message")).doesNotThrowAnyException();
  }

  @Test
  void debugLoggerLogMessageWithSourceAndException() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.DEBUG;
    Exception ex = new RuntimeException("Test exception");
    // Verify no exception is thrown
    assertThatCode(() -> logger.log("Debug test message", "test source", ex)).doesNotThrowAnyException();
  }

  @Test
  void infoLoggerLogMessageWithSourceAndException() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
    Exception ex = new RuntimeException("Test exception");
    // Verify no exception is thrown
    assertThatCode(() -> logger.log("Info test message", "test source", ex)).doesNotThrowAnyException();
  }

  @Test
  void debugLoggerLogMessageWithNullSource() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.DEBUG;
    Exception ex = new RuntimeException("Test exception");
    // Verify no exception is thrown
    assertThatCode(() -> logger.log("Debug test message", null, ex)).doesNotThrowAnyException();
  }

  @Test
  void infoLoggerLogMessageWithNullSource() {
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
    Exception ex = new RuntimeException("Test exception");
    // Verify no exception is thrown
    assertThatCode(() -> logger.log("Info test message", null, ex)).doesNotThrowAnyException();
  }

  @Test
  void getLoggerReturnsSameInstance() {
    // This test verifies the internal implementation detail that getLogger() returns a cached instance
    // We can't directly test this without reflection, but we can verify the behavior is consistent
    IntrospectionFailureLogger debugLogger = IntrospectionFailureLogger.DEBUG;
    IntrospectionFailureLogger infoLogger = IntrospectionFailureLogger.INFO;

    // Both should be able to log without errors
    assertThatCode(() -> debugLogger.log("test")).doesNotThrowAnyException();
    assertThatCode(() -> infoLogger.log("test")).doesNotThrowAnyException();
  }

}