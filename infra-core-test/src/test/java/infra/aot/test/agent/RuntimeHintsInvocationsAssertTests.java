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
import infra.aot.agent.RecordedInvocation;

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
  void shouldReportMissingStaticInvocation() throws Exception {
    RecordedInvocation invocation = invocation("CLASS_FORNAME", null, String.class);
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocations(List.of(invocation)).assertThat();

    assertThatThrownBy(() -> assertions.match(new RuntimeHints()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("java.lang.Class#forName")
            .hasMessageContaining("Stacktrace:");
  }

  @Test
  void shouldReportMissingInstanceInvocation() throws Exception {
    RecordedInvocation invocation = invocation("CLASS_GETMETHOD", String.class, String.class.getMethod("toString"));
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocations(List.of(invocation)).assertThat();

    assertThatThrownBy(() -> assertions.match(new RuntimeHints()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("java.lang.Class#getMethod")
            .hasMessageContaining("java.lang.String")
            .hasMessageContaining("Stacktrace:");
  }

  @Test
  void shouldReportCountMismatch() {
    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocations(List.of()).assertThat();

    assertThatThrownBy(() -> assertions.hasCount(1))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Number of recorded invocations does not match");
  }

  @Test
  void shouldReturnNotMatchingInvocations() throws Exception {
    RecordedInvocation invocation = invocation("CLASS_FORNAME", null, String.class);

    RuntimeHintsInvocationsAssert assertions = new RuntimeHintsInvocations(List.of(invocation)).assertThat();
    assertions.notMatching(new RuntimeHints()).containsExactly(invocation);
  }

  private RecordedInvocation invocation(String instrumentedMethod, Object instance, Object returnValue) throws Exception {
    Class<?> type = Class.forName("infra.aot.agent.InstrumentedMethod");
    var field = type.getDeclaredField(instrumentedMethod);
    field.setAccessible(true);
    Object builder = RecordedInvocation.class.getMethod("of", type).invoke(null, field.get(null));
    if (instance != null) {
      builder = builder.getClass().getMethod("onInstance", Object.class).invoke(builder, instance);
    }
    builder = builder.getClass().getMethod("withArgument", Object.class).invoke(builder, "java.lang.String");
    builder = builder.getClass().getMethod("returnValue", Object.class).invoke(builder, returnValue);
    return (RecordedInvocation) builder.getClass().getMethod("build").invoke(builder);
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
