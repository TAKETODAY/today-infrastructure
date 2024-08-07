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

package cn.taketoday.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.TodayStrategies;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Future#timeout
 * @see java.util.concurrent.Executor
 * @since 5.0 2024/8/5 15:25
 */
public interface Scheduler extends Executor {

  /**
   * Executes the given command at some time in the future.  The command
   * may execute in a new thread, in a pooled thread, or in the calling
   * thread, at the discretion of the {@code Executor} implementation.
   *
   * @param command the runnable task
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   * @throws NullPointerException if command is null
   */
  @Override
  void execute(Runnable command);

  /**
   * Submits a one-shot task that becomes enabled after the given delay.
   *
   * @param command the task to execute
   * @param delay the time from now to delay execution
   * @param unit the time unit of the delay parameter
   * @return a ScheduledFuture representing pending completion of
   * the task and whose {@code get()} method will return
   * {@code null} upon completion
   * @throws RejectedExecutionException if the task cannot be
   * scheduled for execution
   * @throws NullPointerException if command or unit is null
   */
  ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

  /**
   * lookup Scheduler
   */
  static Scheduler lookup() {
    var factory = TodayStrategies.findFirst(SchedulerFactory.class, null);
    if (factory == null) {
      return new DefaultScheduler();
    }
    return factory.create();
  }

}
