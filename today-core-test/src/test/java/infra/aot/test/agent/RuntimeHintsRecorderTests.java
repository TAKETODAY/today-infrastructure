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

import infra.aot.test.agent.RuntimeHintsRecorder.RuntimeHintsInvocationsListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:29
 */
@SuppressWarnings("deprecation")
class RuntimeHintsRecorderTests {

  @Test
  void shouldRecordInvocations() {
    assertThatThrownBy(() -> {
      RuntimeHintsRecorder.record(() -> { });
    }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RuntimeHintsAgent must be loaded");
  }

  @Test
  void shouldHandleNullAction() {
    assertThatThrownBy(() -> {
      RuntimeHintsRecorder.record(null);
    }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Runnable action is required");
  }

  @Test
  void shouldReturnRuntimeHintsInvocations() {
    assertThatThrownBy(() -> {
      RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> { });
      assertThat(invocations).isNotNull();
    }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RuntimeHintsAgent must be loaded");
  }

  @Test
  void shouldHandleEmptyAction() {
    assertThatThrownBy(() -> {
      RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> { });
      assertThat(invocations.recordedInvocations()).isEmpty();
    }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RuntimeHintsAgent must be loaded");
  }

  @Test
  void shouldHandleActionWithException() {
    assertThatThrownBy(() -> {
      RuntimeHintsRecorder.record(() -> {
        throw new RuntimeException("Test exception");
      });
    }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RuntimeHintsAgent must be loaded");
  }

  @Test
  void shouldRequireAgentToBeLoaded() {
    assertThatThrownBy(() -> {
      RuntimeHintsRecorder.record(() -> { });
    }).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RuntimeHintsAgent must be loaded");
  }



}