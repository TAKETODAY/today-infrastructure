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

package infra.scheduling.config;

import java.time.Instant;

import infra.lang.Assert;
import infra.lang.Nullable;
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
