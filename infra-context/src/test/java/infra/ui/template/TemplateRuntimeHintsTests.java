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

package infra.ui.template;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.beans.factory.aot.AotServices;
import infra.core.io.ClassPathResource;
import infra.test.context.FilteredClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/4 18:06
 */
class TemplateRuntimeHintsTests {

  private static final Predicate<RuntimeHints> TEST_PREDICATE = RuntimeHintsPredicates.resource()
          .forResource("templates/something/hello.html");

  @Test
  void templateRuntimeHintsIsRegistered() {
    Iterable<RuntimeHintsRegistrar> registrar = AotServices.factories().load(RuntimeHintsRegistrar.class);
    assertThat(registrar).anyMatch(TemplateRuntimeHints.class::isInstance);
  }

  @Test
  void contributeWhenTemplateLocationExists() {
    RuntimeHints runtimeHints = contribute(getClass().getClassLoader());
    assertThat(TEST_PREDICATE.test(runtimeHints)).isTrue();
  }

  @Test
  void contributeWhenTemplateLocationDoesNotExist() {
    FilteredClassLoader classLoader = new FilteredClassLoader(new ClassPathResource("templates"));
    RuntimeHints runtimeHints = contribute(classLoader);
    assertThat(TEST_PREDICATE.test(runtimeHints)).isFalse();
  }

  private RuntimeHints contribute(ClassLoader classLoader) {
    RuntimeHints runtimeHints = new RuntimeHints();
    new TemplateRuntimeHints().registerHints(runtimeHints, classLoader);
    return runtimeHints;
  }

}