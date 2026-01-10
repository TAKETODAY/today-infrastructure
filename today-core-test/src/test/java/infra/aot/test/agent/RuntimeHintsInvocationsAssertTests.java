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

package infra.aot.test.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:26
 */
class RuntimeHintsInvocationsAssertTests {

  @Test
  void shouldCreateRuntimeHintsInvocationsAssert() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThat(assertions).isNotNull();
  }

  @Test
  void shouldAddRegistrarToConfigurers() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      RuntimeHintsRegistrar registrar = (hints, classLoader) -> { };
      RuntimeHintsInvocationsAssert result = assertions.withRegistrar(registrar);
      assertThat(result).isSameAs(assertions);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldMatchWithRuntimeHints() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      RuntimeHints hints = new RuntimeHints();
      assertions.match(hints);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldReturnNotMatchingListAssert() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      RuntimeHints hints = new RuntimeHints();
      org.assertj.core.api.ListAssert<infra.aot.agent.RecordedInvocation> listAssert = assertions.notMatching(hints);
      assertThat(listAssert).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleNullRuntimeHintsInMatch() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatThrownBy(() -> assertions.match(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RuntimeHints is required");
  }

  @Test
  void shouldHandleNullRuntimeHintsInNotMatching() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatThrownBy(() -> assertions.notMatching(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RuntimeHints is required");
  }

  @Test
  void shouldVerifyZeroCount() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      assertions.hasCount(0);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldAddStrategiesRegistrars() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      RuntimeHintsInvocationsAssert result = assertions.withStrategiesRegistrars("test/location");
      assertThat(result).isSameAs(assertions);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldMatchWithRuntimeHintsAndConfigurers() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    RuntimeHintsRegistrar registrar = (hints, classLoader) -> { };
    assertions.withRegistrar(registrar);

    assertThatCode(() -> {
      RuntimeHints hints = new RuntimeHints();
      assertions.match(hints);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldReturnNotMatchingListAssertWithConfigurers() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    RuntimeHintsRegistrar registrar = (hints, classLoader) -> { };
    assertions.withRegistrar(registrar);

    assertThatCode(() -> {
      RuntimeHints hints = new RuntimeHints();
      org.assertj.core.api.ListAssert<infra.aot.agent.RecordedInvocation> listAssert = assertions.notMatching(hints);
      assertThat(listAssert).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleEmptyInvocationList() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      assertions.hasCount(0);
      RuntimeHints hints = new RuntimeHints();
      assertions.match(hints);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleMultipleRegistrars() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      RuntimeHintsRegistrar registrar1 = (hints, classLoader) -> { };
      RuntimeHintsRegistrar registrar2 = (hints, classLoader) -> { };
      assertions.withRegistrar(registrar1).withRegistrar(registrar2);
      RuntimeHints hints = new RuntimeHints();
      assertions.match(hints);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldChainMultipleWithRegistrarCalls() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    assertThatCode(() -> {
      RuntimeHintsRegistrar registrar1 = (hints, classLoader) -> { };
      RuntimeHintsRegistrar registrar2 = (hints, classLoader) -> { };
      RuntimeHintsRegistrar registrar3 = (hints, classLoader) -> { };
      RuntimeHintsInvocationsAssert result = assertions
              .withRegistrar(registrar1)
              .withRegistrar(registrar2)
              .withRegistrar(registrar3);
      assertThat(result).isSameAs(assertions);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldMatchWithRuntimeHintsAndMultipleConfigurers() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    RuntimeHintsRegistrar registrar1 = (hints, classLoader) -> { };
    RuntimeHintsRegistrar registrar2 = (hints, classLoader) -> { };
    assertions.withRegistrar(registrar1).withRegistrar(registrar2);

    assertThatCode(() -> {
      RuntimeHints hints = new RuntimeHints();
      assertions.match(hints);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldReturnNotMatchingListAssertWithMultipleConfigurers() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocationsAssert(invocations);

    RuntimeHintsRegistrar registrar1 = (hints, classLoader) -> { };
    RuntimeHintsRegistrar registrar2 = (hints, classLoader) -> { };
    assertions.withRegistrar(registrar1).withRegistrar(registrar2);

    assertThatCode(() -> {
      RuntimeHints hints = new RuntimeHints();
      org.assertj.core.api.ListAssert<infra.aot.agent.RecordedInvocation> listAssert = assertions.notMatching(hints);
      assertThat(listAssert).isNotNull();
    }).doesNotThrowAnyException();
  }

}