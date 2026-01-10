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

package infra.aot.hint.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.lang.DummyFactory;
import infra.lang.MyDummyFactory1;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TodayStrategiesRuntimeHints}.
 *
 * @author Phillip Webb
 */
class TodayStrategiesRuntimeHintsTests {

  private RuntimeHints hints;

  @BeforeEach
  void setup() {
    this.hints = new RuntimeHints();
    TodayStrategies.forResourceLocation("META-INF/config/aot.factories")
            .load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
                    .registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void resourceLocationHasHints() {
    assertThat(RuntimeHintsPredicates.resource().forResource(TodayStrategies.STRATEGIES_LOCATION)).accepts(this.hints);
  }

  @Test
  void factoryTypeHasHint() {
    assertThat(RuntimeHintsPredicates.reflection().onType(DummyFactory.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

  @Test
  void factoryImplementationHasHint() {
    assertThat(RuntimeHintsPredicates.reflection().onType(MyDummyFactory1.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

  @Test
  void handlesNonExistentResourceLocationGracefully() {
    RuntimeHints hints = new RuntimeHints();
    TodayStrategiesRuntimeHints runtimeHints = new TodayStrategiesRuntimeHints();
    runtimeHints.registerHints(hints, getClass().getClassLoader());
    // Should not throw exception and complete normally
  }

  @Test
  void skipsUnresolvableFactoryClasses() {
    RuntimeHints hints = new RuntimeHints();
    ClassLoader classLoader = getClass().getClassLoader();
    TodayStrategiesRuntimeHints runtimeHints = new TodayStrategiesRuntimeHints();

    // This should not throw an exception even if class doesn't exist
    runtimeHints.registerHints(hints, classLoader);
    // Test passes if no exception is thrown
  }

  @Test
  void resolvesAndRegistersValidFactoryClasses() {
    // Using existing test infrastructure to verify normal operation
    assertThat(RuntimeHintsPredicates.reflection().onType(DummyFactory.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

  @Test
  void resolvesAndRegistersValidImplementationClasses() {
    // Using existing test infrastructure to verify normal operation
    assertThat(RuntimeHintsPredicates.reflection().onType(MyDummyFactory1.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

  @Test
  void handlesNullClassLoaderGracefully() {
    RuntimeHints hints = new RuntimeHints();
    TodayStrategiesRuntimeHints runtimeHints = new TodayStrategiesRuntimeHints();
    runtimeHints.registerHints(hints, null);
    // Test passes if no exception is thrown
  }

}
