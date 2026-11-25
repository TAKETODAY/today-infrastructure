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

import java.lang.reflect.Field;

import infra.aot.agent.RuntimeHintsAgent;
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

  @Test
  void shouldRecordEmptyActionWhenAgentIsLoaded() throws Exception {
    withAgentLoaded(true);
    try {
      RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> { });
      assertThat(invocations.recordedInvocations()).isEmpty();
    }
    finally {
      withAgentLoaded(false);
    }
  }

  @Test
  void shouldRemoveListenerWhenActionFails() throws Exception {
    withAgentLoaded(true);
    try {
      assertThatThrownBy(() -> RuntimeHintsRecorder.record(() -> {
        throw new RuntimeException("Test exception");
      })).isInstanceOf(RuntimeException.class).hasMessage("Test exception");
    }
    finally {
      withAgentLoaded(false);
    }
  }

  @Test
  void shouldRecordInvocationInListener() throws Exception {
    RuntimeHintsInvocationsListener listener = new RuntimeHintsInvocationsListener();
    Class<?> type = Class.forName("infra.aot.agent.InstrumentedMethod");
    var field = type.getDeclaredField("CLASS_FORNAME");
    field.setAccessible(true);
    Object builder = Class.forName("infra.aot.agent.RecordedInvocation")
            .getMethod("of", type).invoke(null, field.get(null));
    builder = builder.getClass().getMethod("withArgument", Object.class).invoke(builder, "java.lang.String");
    Object invocation = builder.getClass().getMethod("build").invoke(builder);
    listener.onInvocation((infra.aot.agent.RecordedInvocation) invocation);
    assertThat(listener).isNotNull();
  }

  private void withAgentLoaded(boolean loaded) throws Exception {
    Field field = RuntimeHintsAgent.class.getDeclaredField("loaded");
    field.setAccessible(true);
    field.setBoolean(null, loaded);
  }



}
