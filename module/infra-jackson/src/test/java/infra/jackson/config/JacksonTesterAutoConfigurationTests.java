/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jackson.config;

import org.junit.jupiter.api.Test;

import infra.aot.hint.predicate.ReflectionHintsPredicates;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.app.test.config.json.JsonTestersAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.json.JacksonTester;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonTestersAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JacksonTesterAutoConfigurationTests {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(JsonTestersAutoConfiguration.class, JacksonAutoConfiguration.class,
                  JacksonTesterTestAutoConfiguration.class));

  @Test
  void hintsAreContributed() {
    this.runner.withPropertyValues("infra.test.jsontesters.enabled=true").prepare((context) -> {
      TestGenerationContext generationContext = new TestGenerationContext();
      new ApplicationContextAotGenerator().processAheadOfTime(
              (GenericApplicationContext) context.getSourceApplicationContext(), generationContext);
      ReflectionHintsPredicates hints = RuntimeHintsPredicates.reflection();
      assertThat(hints.onType(JacksonTester.class)).accepts(generationContext.getRuntimeHints());
    });
  }

}
