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

package infra.scheduling.config;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

import infra.lang.Assert;
import infra.scheduling.SchedulingAwareRunnable;

/**
 * Holder class defining a {@code Runnable} to be executed as a task, typically at a
 * scheduled time or interval. See subclass hierarchy for various scheduling approaches.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Task {

  private final Runnable runnable;

  private TaskExecutionOutcome lastExecutionOutcome;

  /**
   * Create a new {@code Task}.
   *
   * @param runnable the underlying task to execute
   */
  public Task(Runnable runnable) {
    Assert.notNull(runnable, "Runnable is required");
    this.runnable = new OutcomeTrackingRunnable(runnable);
    this.lastExecutionOutcome = TaskExecutionOutcome.create();
  }

  /**
   * Return a {@link Runnable} that executes the underlying task.
   * <p>Note, this does not necessarily return the {@link Task#Task(Runnable) original runnable}
   * as it can be wrapped by the Framework for additional support.
   */
  public Runnable getRunnable() {
    return this.runnable;
  }

  /**
   * Return the outcome of the last task execution.
   *
   * @since 6.2
   */
  public TaskExecutionOutcome getLastExecutionOutcome() {
    return this.lastExecutionOutcome;
  }

  @Override
  public String toString() {
    return this.runnable.toString();
  }

  private class OutcomeTrackingRunnable implements SchedulingAwareRunnable {

    private final Runnable runnable;

    public OutcomeTrackingRunnable(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void run() {
      try {
        Task.this.lastExecutionOutcome = Task.this.lastExecutionOutcome.start(Instant.now());
        this.runnable.run();
        Task.this.lastExecutionOutcome = Task.this.lastExecutionOutcome.success();
      }
      catch (Throwable exc) {
        Task.this.lastExecutionOutcome = Task.this.lastExecutionOutcome.failure(exc);
        throw exc;
      }
    }

    @Override
    public boolean isLongLived() {
      if (this.runnable instanceof SchedulingAwareRunnable sar) {
        return sar.isLongLived();
      }
      return SchedulingAwareRunnable.super.isLongLived();
    }

    @Override
    @Nullable
    public String getQualifier() {
      if (this.runnable instanceof SchedulingAwareRunnable sar) {
        return sar.getQualifier();
      }
      return null;
    }

    @Override
    public String toString() {
      return this.runnable.toString();
    }
  }

}
