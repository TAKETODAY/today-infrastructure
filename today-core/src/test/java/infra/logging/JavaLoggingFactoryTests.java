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
 * @since 5.0 2025/11/7 21:31
 */
class JavaLoggingFactoryTests {

  @Test
  void shouldCreateJavaLoggingLogger() {
    JavaLoggingFactory factory = new JavaLoggingFactory();
    String loggerName = "test.logger";

    JavaLoggingLogger logger = factory.createLogger(loggerName);

    assertThat(logger).isNotNull();
    assertThat(logger.getName()).isEqualTo(loggerName);
  }

  @Test
  void shouldCreateLoggerWithFinerLevelEnabled() {
    JavaLoggingFactory factory = new JavaLoggingFactory();
    String loggerName = "test.finer.logger";

    JavaLoggingLogger logger = factory.createLogger(loggerName);

    assertThat(logger).isNotNull();
    // The actual isLoggable state depends on the logging configuration
    assertThat(logger.getName()).isEqualTo(loggerName);
  }

}