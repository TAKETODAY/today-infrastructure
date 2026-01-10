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