/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.scheduling.config;

import java.time.Instant;

import cn.taketoday.lang.Assert;

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
    Assert.notNull(runnable, "Runnable must not be null");
    this.runnable = new OutcomeTrackingRunnable(runnable);
    this.lastExecutionOutcome = TaskExecutionOutcome.create();
  }

  /**
   * Return the underlying task.
   */
  public Runnable getRunnable() {
    return this.runnable;
  }

  /**
   * Return the outcome of the last task execution.
   *
   * @since 5.0
   */
  public TaskExecutionOutcome getLastExecutionOutcome() {
    return this.lastExecutionOutcome;
  }

  @Override
  public String toString() {
    return this.runnable.toString();
  }

  private class OutcomeTrackingRunnable implements Runnable {

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
    public String toString() {
      return this.runnable.toString();
    }
  }

}
