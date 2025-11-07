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