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

package infra.app.test.config.json;

import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.ReflectionHintsPredicates;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.app.test.json.BasicJsonTester;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonTestersAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JsonTestersAutoConfigurationTests {

  @Test
  void basicJsonTesterHintsAreContributed() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      TestPropertyValues.of("infra.test.jsontesters.enabled=true").applyTo(context);
      context.register(JsonTestersAutoConfiguration.class);
      TestGenerationContext generationContext = new TestGenerationContext();
      new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
      RuntimeHints runtimeHints = generationContext.getRuntimeHints();
      ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
      assertThat(reflection.onType(BasicJsonTester.class)).accepts(runtimeHints);
    }
  }

}
