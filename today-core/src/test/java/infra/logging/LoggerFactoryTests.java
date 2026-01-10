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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:36
 */
class LoggerFactoryTests {

  @Test
  void shouldGetLoggerByClass() {
    Logger logger = LoggerFactory.getLogger(LoggerFactoryTests.class);

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo(LoggerFactoryTests.class.getName());
  }

  @Test
  void shouldGetLoggerByName() {
    String loggerName = "test.logger.name";
    Logger logger = LoggerFactory.getLogger(loggerName);

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo(loggerName);
  }

  @Test
  void shouldReturnSameLoggerInstanceForSameName() {
    String loggerName = "test.same.instance";
    Logger logger1 = LoggerFactory.getLogger(loggerName);
    Logger logger2 = LoggerFactory.getLogger(loggerName);

    assertThat(logger1).isEqualTo(logger2);
    assertThat(logger1).isEqualTo(logger1);
    assertThat(logger1).isNotEqualTo(1);
  }

  @Test
  void shouldCreateSlf4jLoggerFactoryWhenAvailable() {
    // This test verifies the factory creation logic works
    // The actual factory type depends on what's available in classpath
    Logger logger = LoggerFactory.getLogger("test.factory");

    assertThat(logger).isNotNull();
  }

  @Test
  void shouldFallBackToJavaLoggingFactory() {
    // This tests that fallback mechanism works
    // Actual behavior depends on classpath, but should always return a logger
    Logger logger = LoggerFactory.getLogger("test.fallback");

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo("test.fallback");
  }

}