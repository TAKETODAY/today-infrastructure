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

package infra.scheduling;

import infra.core.task.TaskExecutor;
import infra.lang.Nullable;
import infra.scheduling.annotation.Scheduled;
import infra.scheduling.config.TaskSchedulerRouter;

/**
 * Extension of the {@link Runnable} interface, adding special callbacks
 * for long-running operations.
 *
 * <p>Scheduling-capable TaskExecutors are encouraged to check a submitted
 * Runnable, detecting whether this interface is implemented and reacting
 * as appropriately as they are able to.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TaskExecutor
 * @see SchedulingTaskExecutor
 * @since 4.0
 */
public interface SchedulingAwareRunnable extends Runnable {

  /**
   * Return whether the Runnable's operation is long-lived
   * ({@code true}) versus short-lived ({@code false}).
   * <p>In the former case, the task will not allocate a thread from the thread
   * pool (if any) but rather be considered as long-running background thread.
   * <p>This should be considered a hint. Of course TaskExecutor implementations
   * are free to ignore this flag and the SchedulingAwareRunnable interface overall.
   * <p>The default implementation returns {@code false}
   */
  default boolean isLongLived() {
    return false;
  }

  /**
   * Return a qualifier associated with this Runnable.
   * <p>The default implementation returns {@code null}.
   * <p>May be used for custom purposes depending on the scheduler implementation.
   * {@link TaskSchedulerRouter} introspects
   * this qualifier in order to determine the target scheduler to be used
   * for a given Runnable, matching the qualifier value (or the bean name)
   * of a specific {@link TaskScheduler} or
   * {@link java.util.concurrent.ScheduledExecutorService} bean definition.
   *
   * @see Scheduled#scheduler()
   */
  @Nullable
  String getQualifier();
}
