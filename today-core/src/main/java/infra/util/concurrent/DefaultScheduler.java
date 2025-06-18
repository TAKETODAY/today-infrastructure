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

package infra.util.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import infra.lang.TodayStrategies;

/**
 * A default implementation of the {@link Scheduler} interface that provides
 * task execution and scheduling capabilities using a combination of a
 * {@link ForkJoinPool} and a {@link ScheduledExecutorService}.
 *
 * <p>This class uses the common {@link ForkJoinPool} for immediate task execution
 * and a {@link ScheduledThreadPoolExecutor} for delayed task scheduling. The maximum
 * pool size for the scheduled thread pool can be configured via the system property
 * {@code infra.util.concurrent.Scheduler.maximumPoolSize}.
 *
 *
 * <p><b>Configuration:</b>
 *
 * <p>The maximum pool size for the scheduled thread pool can be customized by setting
 * the system property {@code infra.util.concurrent.Scheduler.maximumPoolSize}. If not
 * specified, the default value is {@code 2}.
 *
 * <p><b>Note:</b> This class is final and cannot be extended. It is designed to provide
 * a simple and efficient default implementation of the {@code Scheduler} interface.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Scheduler
 * @see ForkJoinPool
 * @see ScheduledThreadPoolExecutor
 * @since 5.0 2024/8/7 11:20
 */
final class DefaultScheduler implements Scheduler {

  /**
   * The system property key used to configure the maximum pool size for the
   * scheduled thread pool in the {@link DefaultScheduler} class.
   *
   * <p>This property allows customization of the maximum number of threads that
   * can be used by the scheduled thread pool. If this property is not set, the
   * default value of {@code 2} will be used.
   *
   * <p><b>Usage Example:</b>
   *
   * <p>To configure the maximum pool size, you can set the system property before
   * initializing the scheduler:
   * <pre>{@code
   * System.setProperty(MaximumPoolSize, "5");
   * Scheduler scheduler = new DefaultScheduler();
   * }</pre>
   *
   * <p>In this example, the scheduled thread pool will be configured with a maximum
   * pool size of 5 threads.
   */
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
