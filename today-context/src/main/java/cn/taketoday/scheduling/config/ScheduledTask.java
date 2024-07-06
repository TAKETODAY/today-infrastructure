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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Nullable;

/**
 * A representation of a scheduled task at runtime,
 * used as a return value for scheduling methods.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScheduledTaskRegistrar#scheduleCronTask(CronTask)
 * @see ScheduledTaskRegistrar#scheduleFixedRateTask(FixedRateTask)
 * @see ScheduledTaskRegistrar#scheduleFixedDelayTask(FixedDelayTask)
 * @since 4.0
 */
public final class ScheduledTask {

  private final Task task;

  @Nullable
  volatile ScheduledFuture<?> future;

  ScheduledTask(Task task) {
    this.task = task;
  }

  /**
   * Return the underlying task (typically a {@link CronTask},
   * {@link FixedRateTask} or {@link FixedDelayTask}).
   */
  public Task getTask() {
    return this.task;
  }

  /**
   * Trigger cancellation of this scheduled task.
   * <p>This variant will force interruption of the task if still running.
   *
   * @see #cancel(boolean)
   */
  public void cancel() {
    cancel(true);
  }

  /**
   * Trigger cancellation of this scheduled task.
   *
   * @param mayInterruptIfRunning whether to force interruption of the task
   * if still running (specify {@code false} to allow the task to complete)
   * @see ScheduledFuture#cancel(boolean)
   */
  public void cancel(boolean mayInterruptIfRunning) {
    ScheduledFuture<?> future = this.future;
    if (future != null) {
      future.cancel(mayInterruptIfRunning);
    }
  }

  /**
   * Return the next scheduled execution of the task, or {@code null}
   * if the task has been cancelled or no new execution is scheduled.
   *
   * @since 5.0
   */
  @Nullable
  public Instant nextExecution() {
    ScheduledFuture<?> future = this.future;
    if (future != null && !future.isCancelled()) {
      long delay = future.getDelay(TimeUnit.MILLISECONDS);
      if (delay > 0) {
        return Instant.now().plusMillis(delay);
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return this.task.toString();
  }

}
