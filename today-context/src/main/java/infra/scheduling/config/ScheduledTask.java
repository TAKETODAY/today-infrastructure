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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
