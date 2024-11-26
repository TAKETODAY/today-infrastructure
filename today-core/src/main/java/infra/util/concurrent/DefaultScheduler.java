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

package infra.util.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import infra.lang.TodayStrategies;

/**
 * Default scheduler
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/8/7 11:20
 */
final class DefaultScheduler implements Scheduler {

  static final String MaximumPoolSize = "infra.util.concurrent.Scheduler.maximumPoolSize";

  private final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

  private final ScheduledExecutorService scheduledThreadPool;

  DefaultScheduler() {
    var executor = new ScheduledThreadPoolExecutor(1);
    executor.setMaximumPoolSize(TodayStrategies.getInt(MaximumPoolSize, 2));
    executor.setRemoveOnCancelPolicy(true);
    this.scheduledThreadPool = executor;
  }

  @Override
  public void execute(Runnable command) {
    forkJoinPool.execute(command);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return scheduledThreadPool.schedule(command, delay, unit);
  }

}
