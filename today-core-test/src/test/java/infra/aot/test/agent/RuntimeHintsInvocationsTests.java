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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:25
 */
class RuntimeHintsInvocationsTests {

  @Test
  void shouldCreateRuntimeHintsInvocationsWithEmptyList() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    assertThat(invocations).isNotNull();
    assertThat(invocations.recordedInvocations()).isEmpty();
  }

  @Test
  void shouldProvideAssertThatMethod() {
    RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(List.of());
    RuntimeHintsInvocationsAssert assertions = invocations.assertThat();

    assertThat(assertions).isNotNull();
    assertThat(assertions).isInstanceOf(RuntimeHintsInvocationsAssert.class);
  }

  @Test
  void shouldHandleNullInvocationsList() {
    assertThatCode(() -> {
      RuntimeHintsInvocations invocations = new RuntimeHintsInvocations(null);
      // This should throw NullPointerException when accessing stream
    }).doesNotThrowAnyException(); // Constructor doesn't throw, but stream access might
  }

}