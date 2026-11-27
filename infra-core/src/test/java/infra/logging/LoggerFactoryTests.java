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

import infra.test.classpath.ClassPathExclusions;

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
    Logger logger = LoggerFactory.getLogger("test.factory");

    assertThat(logger).isInstanceOf(Slf4jLogger.class);
  }

  @Test
  @ClassPathExclusions("slf4j-api-*.jar")
  void shouldFallBackToLog4j2WhenSlf4jIsAbsent() {
    assertThat(LoggerFactory.getLogger("test.fallback.log4j2")).isInstanceOf(Log4j2Logger.class);
  }

  @Test
  @ClassPathExclusions({ "slf4j-api-*.jar", "log4j-api-*.jar" })
  void shouldFallBackToJavaLoggingFactory() {
    Logger logger = LoggerFactory.getLogger("test.fallback");

    assertThat(logger).isInstanceOf(JavaLoggingLogger.class);
    assertThat(logger.getName()).isEqualTo("test.fallback");
  }

}
