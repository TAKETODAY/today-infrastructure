/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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