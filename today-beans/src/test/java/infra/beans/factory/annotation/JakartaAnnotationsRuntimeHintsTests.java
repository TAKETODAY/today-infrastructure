/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.beans.factory.aot.AotServices;
import infra.util.ClassUtils;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 20:09
 */
class JakartaAnnotationsRuntimeHintsTests {

  private final RuntimeHints hints = new RuntimeHints();

  @BeforeEach
  void setup() {
    AotServices.factories().load(RuntimeHintsRegistrar.class)
            .forEach(registrar -> registrar.registerHints(this.hints,
                    ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void jakartaInjectAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Inject.class)).accepts(this.hints);
  }

  @Test
  void jakartaProviderAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Provider.class)).accepts(this.hints);
  }

  @Test
  void jakartaQualifierAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Qualifier.class)).accepts(this.hints);
  }

}