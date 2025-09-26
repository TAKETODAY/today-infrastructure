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

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import infra.lang.TodayStrategies;

/**
 * An interface representing a scheduler capable of executing tasks asynchronously
 * or after a specified delay. It extends the {@link Executor} interface to provide
 * additional scheduling capabilities.
 *
 * <p>The {@code Scheduler} interface allows tasks to be executed either immediately
 * or after a delay using a thread pool or other execution mechanisms. Implementations
 * of this interface can define their own strategies for task execution and thread
 * management.
 *
 * <p><b>Usage Examples:</b>
 *
 * <p>1. Creating a custom scheduler implementation:
 * <pre>{@code
 * static class MyScheduler implements Scheduler {
 *   private final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
 *
 *   @Override
 *   public void execute(Runnable command) {
 *     scheduledThreadPool.execute(command);
 *   }
 *
 *   @Override
 *   public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
 *     return scheduledThreadPool.schedule(command, delay, unit);
 *   }
 * }
 * }</pre>
 *
 * <p>2. Using the default scheduler:
 * <pre>{@code
 * Scheduler scheduler = Scheduler.lookup();
 * scheduler.execute(() -> System.out.println("Task executed"));
 * }</pre>
 *
 * <p>3. Scheduling a task with a delay:
 * <pre>{@code
 * Scheduler scheduler = Scheduler.lookup();
 * scheduler.schedule(() -> System.out.println("Delayed task executed"), 5, TimeUnit.SECONDS);
 * }</pre>
 *
 * <p>4. Combining with a ForkJoinPool for task execution:
 * <pre>{@code
 * static class CombinedScheduler implements Scheduler {
 *   private final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
 *   private final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
 *
 *   @Override
 *   public void execute(Runnable command) {
 *     forkJoinPool.execute(command);
 *   }
 *
 *   @Override
 *   public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
 *     return scheduledThreadPool.schedule(command, delay, unit);
 *   }
 * }
 * }</pre>
 *
 * <p><b>Note:</b> The {@link #lookup()} method provides a convenient way to obtain
 * an instance of a {@code Scheduler}. It uses a strategy pattern to locate an
 * appropriate implementation, falling back to a default implementation if none is found.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Future#timeout
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ScheduledExecutorService
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
   * Looks up and returns an instance of {@code Scheduler} using a predefined strategy.
   *
   * <p>The method first attempts to find a {@code SchedulerFactory} instance. If found,
   * it uses the factory to create and return a {@code Scheduler}. If no factory is found,
   * it then looks for a direct {@code Scheduler} instance. If neither a factory nor a
   * scheduler is found, a default scheduler implementation ({@code DefaultScheduler}) is
   * returned.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Scheduler scheduler = Scheduler.lookup();
   *   scheduler.execute(() -> {
   *     System.out.println("Task executed by the scheduler.");
   *   });
   * }</pre>
   *
   * <p>This method is useful in scenarios where a scheduler needs to be dynamically
   * resolved based on available implementations or fallback strategies.
   *
   * @return an instance of {@code Scheduler}, either created by a factory, retrieved
   * directly, or as a default implementation if no other options are available
   */
  static Scheduler lookup() {
    var factory = TodayStrategies.findFirst(SchedulerFactory.class, null);
    if (factory == null) {
      Scheduler scheduler = TodayStrategies.findFirst(Scheduler.class, null);
      if (scheduler == null) {
        return new DefaultScheduler();
      }
      return scheduler;
    }
    return factory.create();
  }

}
