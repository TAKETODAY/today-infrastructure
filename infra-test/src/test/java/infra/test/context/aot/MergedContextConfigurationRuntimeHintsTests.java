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

package infra.test.context.aot;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class MergedContextConfigurationRuntimeHintsTests {

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "WARN", "OFF" })
  void onlyExistingConcreteClasspathResourcesReceiveHints(LogLevel level) {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = MergedContextConfigurationRuntimeHints.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      String path = "infra/test/context/jdbc/schema.sql";
      var configuration = new MergedContextConfiguration(getClass(), new String[] {
              "classpath:" + path, "classpath:missing.sql", "file:/missing.sql",
              "classpath*:schema.sql", "classpath:*.sql", "classpath:?.sql" }, null, null, null);
      RuntimeHints hints = new RuntimeHints();
      new MergedContextConfigurationRuntimeHints().registerHints(hints, configuration, getClass().getClassLoader());
      assertThat(RuntimeHintsPredicates.resource().forResource(path)).accepts(hints);
      assertThat(RuntimeHintsPredicates.resource().forResource("missing.sql")).rejects(hints);
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

}
