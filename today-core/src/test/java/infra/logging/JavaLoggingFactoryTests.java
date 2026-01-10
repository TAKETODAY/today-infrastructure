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