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

import java.util.ArrayDeque;
import java.util.Deque;

import infra.aot.agent.RecordedInvocation;
import infra.aot.agent.RecordedInvocationsListener;
import infra.aot.agent.RecordedInvocationsPublisher;
import infra.aot.agent.RuntimeHintsAgent;
import infra.aot.hint.RuntimeHints;
import infra.lang.Assert;

/**
 * Invocations relevant to {@link RuntimeHints} recorded during the execution of a block
 * of code instrumented by the {@link RuntimeHintsAgent}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 * @deprecated in favor of the {@code -XX:MissingRegistrationReportingMode=Warn} and
 * {@code -XX:MissingRegistrationReportingMode=Exit} JVM flags with GraalVM.
 */
@Deprecated
public final class RuntimeHintsRecorder {

  private final RuntimeHintsInvocationsListener listener;

  private RuntimeHintsRecorder() {
    this.listener = new RuntimeHintsInvocationsListener();
  }

  /**
   * Record all method invocations relevant to {@link RuntimeHints} that happened
   * during the execution of the given action.
   *
   * @param action the block of code we want to record invocations from
   * @return the recorded invocations
   */
  public static synchronized RuntimeHintsInvocations record(Runnable action) {
    Assert.notNull(action, "Runnable action is required");
    Assert.state(RuntimeHintsAgent.isLoaded(), "RuntimeHintsAgent must be loaded in the current JVM");
    RuntimeHintsRecorder recorder = new RuntimeHintsRecorder();
    RecordedInvocationsPublisher.addListener(recorder.listener);
    try {
      action.run();
    }
    finally {
      RecordedInvocationsPublisher.removeListener(recorder.listener);
    }
    return new RuntimeHintsInvocations(recorder.listener.recordedInvocations.stream().toList());
  }

  static final class RuntimeHintsInvocationsListener implements RecordedInvocationsListener {

    private final Deque<RecordedInvocation> recordedInvocations = new ArrayDeque<>();

    @Override
    public void onInvocation(RecordedInvocation invocation) {
      this.recordedInvocations.addLast(invocation);
    }

  }

}
