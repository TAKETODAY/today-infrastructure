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

package cn.taketoday.scheduling.concurrent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.core.task.AsyncListenableTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskRejectedException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.SchedulingTaskExecutor;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.support.TaskUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ErrorHandler;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.util.concurrent.ListenableFutureTask;

/**
 * A standard implementation of Infra {@link TaskScheduler} interface, wrapping
 * a native {@link java.util.concurrent.ScheduledThreadPoolExecutor} and providing
 * all applicable configuration options for it.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
 * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
 * @see #setThreadFactory
 * @see #setErrorHandler
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
        implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, TaskScheduler {

  private static final TimeUnit NANO = TimeUnit.NANOSECONDS;

  private volatile int poolSize = 1;

  private volatile boolean removeOnCancelPolicy;

  private volatile boolean continueExistingPeriodicTasksAfterShutdownPolicy;

  private volatile boolean executeExistingDelayedTasksAfterShutdownPolicy = true;

  @Nullable
  private TaskDecorator taskDecorator;

  @Nullable
  private volatile ErrorHandler errorHandler;

  private Clock clock = Clock.systemDefaultZone();

  @Nullable
  private ScheduledExecutorService scheduledExecutor;

  // Underlying ScheduledFutureTask to user-level ListenableFuture handle, if any
  private final ConcurrentReferenceHashMap<Object, Future<?>> listenableFutureMap =
          new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

  /**
   * Set the ScheduledExecutorService's pool size.
   * Default is 1.
   * <p><b>This setting can be modified at runtime, for example through JMX.</b>
   */
  public void setPoolSize(int poolSize) {
    Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
    if (scheduledExecutor instanceof ScheduledThreadPoolExecutor tpe) {
      tpe.setCorePoolSize(poolSize);
    }
    this.poolSize = poolSize;
  }

  /**
   * Set the remove-on-cancel mode on {@link ScheduledThreadPoolExecutor}.
   * <p>Default is {@code false}. If set to {@code true}, the target executor will be
   * switched into remove-on-cancel mode (if possible).
   * <p><b>This setting can be modified at runtime, for example through JMX.</b>
   *
   * @see ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy
   */
  public void setRemoveOnCancelPolicy(boolean flag) {
    if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor stpe) {
      stpe.setRemoveOnCancelPolicy(flag);
    }
    this.removeOnCancelPolicy = flag;
  }

  /**
   * Set whether to continue existing periodic tasks even when this executor has been shutdown.
   * <p>Default is {@code false}. If set to {@code true}, the target executor will be
   * switched into continuing periodic tasks (if possible).
   * <p><b>This setting can be modified at runtime, for example through JMX.</b>
   *
   * @see ScheduledThreadPoolExecutor#setContinueExistingPeriodicTasksAfterShutdownPolicy
   */
  public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean flag) {
    if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor tpe) {
      tpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(flag);
    }
    this.continueExistingPeriodicTasksAfterShutdownPolicy = flag;
  }

  /**
   * Set whether to execute existing delayed tasks even when this executor has been shutdown.
   * <p>Default is {@code true}. If set to {@code false}, the target executor will be
   * switched into dropping remaining tasks (if possible).
   * <p><b>This setting can be modified at runtime, for example through JMX.</b>
   *
   * @see ScheduledThreadPoolExecutor#setExecuteExistingDelayedTasksAfterShutdownPolicy
   */
  public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean flag) {
    if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor tpe) {
      tpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(flag);
    }
    this.executeExistingDelayedTasksAfterShutdownPolicy = flag;
  }

  /**
   * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
   * about to be executed.
   * <p>Note that such a decorator is not being applied to the user-supplied
   * {@code Runnable}/{@code Callable} but rather to the scheduled execution
   * callback (a wrapper around the user-supplied task).
   * <p>The primary use case is to set some execution context around the task's
   * invocation, or to provide some monitoring/statistics for task execution.
   */
  public void setTaskDecorator(TaskDecorator taskDecorator) {
    this.taskDecorator = taskDecorator;
  }

  /**
   * Set a custom {@link ErrorHandler} strategy.
   */
  public void setErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  /**
   * Set the clock to use for scheduling purposes.
   * <p>The default clock is the system clock for the default time zone.
   *
   * @see Clock#systemDefaultZone()
   */
  public void setClock(Clock clock) {
    Assert.notNull(clock, "Clock is required");
    this.clock = clock;
  }

  @Override
  public Clock getClock() {
    return this.clock;
  }

  @Override
  protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
    ScheduledExecutorService executor = createExecutor(this.poolSize, threadFactory, rejectedHandler);
    if (executor instanceof ScheduledThreadPoolExecutor tpExecutor) {
      if (this.removeOnCancelPolicy) {
        tpExecutor.setRemoveOnCancelPolicy(true);
      }
      if (this.continueExistingPeriodicTasksAfterShutdownPolicy) {
        tpExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
      }
      if (!this.executeExistingDelayedTasksAfterShutdownPolicy) {
        tpExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      }
    }
    this.scheduledExecutor = executor;
    return executor;
  }

  /**
   * Create a new {@link ScheduledExecutorService} instance.
   * <p>The default implementation creates a {@link ScheduledThreadPoolExecutor}.
   * Can be overridden in subclasses to provide custom {@link ScheduledExecutorService} instances.
   *
   * @param poolSize the specified pool size
   * @param threadFactory the ThreadFactory to use
   * @param rejectedHandler the RejectedExecutionHandler to use
   * @return a new ScheduledExecutorService instance
   * @see #afterPropertiesSet()
   * @see java.util.concurrent.ScheduledThreadPoolExecutor
   */
  protected ScheduledExecutorService createExecutor(int poolSize,
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {

    return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedHandler) {
      @Override
      protected void beforeExecute(Thread thread, Runnable task) {
        ThreadPoolTaskScheduler.this.beforeExecute(thread, task);
      }

      @Override
      protected void afterExecute(Runnable task, Throwable ex) {
        ThreadPoolTaskScheduler.this.afterExecute(task, ex);
      }

      @Override
      protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return decorateTaskIfNecessary(task);
      }

      @Override
      protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return decorateTaskIfNecessary(task);
      }
    };
  }

  /**
   * Return the underlying ScheduledExecutorService for native access.
   *
   * @return the underlying ScheduledExecutorService (never {@code null})
   * @throws IllegalStateException if the ThreadPoolTaskScheduler hasn't been initialized yet
   */
  public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
    Assert.state(scheduledExecutor != null, "ThreadPoolTaskScheduler not initialized");
    return scheduledExecutor;
  }

  /**
   * Return the underlying ScheduledThreadPoolExecutor, if available.
   *
   * @return the underlying ScheduledExecutorService (never {@code null})
   * @throws IllegalStateException if the ThreadPoolTaskScheduler hasn't been initialized yet
   * or if the underlying ScheduledExecutorService isn't a ScheduledThreadPoolExecutor
   * @see #getScheduledExecutor()
   */
  public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() throws IllegalStateException {
    Assert.state(scheduledExecutor instanceof ScheduledThreadPoolExecutor,
            "No ScheduledThreadPoolExecutor available");
    return (ScheduledThreadPoolExecutor) scheduledExecutor;
  }

  /**
   * Return the current pool size.
   * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
   *
   * @see #getScheduledThreadPoolExecutor()
   * @see java.util.concurrent.ScheduledThreadPoolExecutor#getPoolSize()
   */
  public int getPoolSize() {
    if (scheduledExecutor == null) {
      // Not initialized yet: assume initial pool size.
      return poolSize;
    }
    return getScheduledThreadPoolExecutor().getPoolSize();
  }

  /**
   * Return the number of currently active threads.
   * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
   *
   * @see #getScheduledThreadPoolExecutor()
   * @see java.util.concurrent.ScheduledThreadPoolExecutor#getActiveCount()
   */
  public int getActiveCount() {
    if (scheduledExecutor == null) {
      // Not initialized yet: assume no active threads.
      return 0;
    }
    return getScheduledThreadPoolExecutor().getActiveCount();
  }

  /**
   * Return the current setting for the remove-on-cancel mode.
   * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
   */
  public boolean isRemoveOnCancelPolicy() {
    if (scheduledExecutor == null) {
      // Not initialized yet: return our setting for the time being.
      return removeOnCancelPolicy;
    }
    return getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy();
  }

  // SchedulingTaskExecutor implementation

  @Override
  public void execute(Runnable task) {
    Executor executor = getScheduledExecutor();
    try {
      executor.execute(errorHandlingTask(task, false));
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public java.util.concurrent.Future<?> submit(Runnable task) {
    ExecutorService executor = getScheduledExecutor();
    try {
      return executor.submit(errorHandlingTask(task, false));
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public <T> java.util.concurrent.Future<T> submit(Callable<T> task) {
    ExecutorService executor = getScheduledExecutor();
    try {
      return executor.submit(new DelegatingErrorHandlingCallable<>(task, this.errorHandler));
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public Future<?> submitListenable(Runnable task) {
    ExecutorService executor = getScheduledExecutor();
    try {
      var future = new ListenableFutureTask<>(executor, task, null);
      executeAndTrack(executor, future);
      return future;
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public <T> Future<T> submitListenable(Callable<T> task) {
    ExecutorService executor = getScheduledExecutor();
    try {
      var future = new ListenableFutureTask<>(executor, task);
      executeAndTrack(executor, future);
      return future;
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  private void executeAndTrack(ExecutorService executor, ListenableFutureTask<?> task) {
    var scheduledFuture = executor.submit(errorHandlingTask(task, false));
    listenableFutureMap.put(scheduledFuture, task);
    task.onCompleted(f -> listenableFutureMap.remove(scheduledFuture));
  }

  @Override
  protected void cancelRemainingTask(Runnable task) {
    super.cancelRemainingTask(task);
    // Cancel associated user-level ListenableFuture handle as well
    Future<?> future = this.listenableFutureMap.get(task);
    if (future != null) {
      future.cancel(true);
    }
  }

  // TaskScheduler implementation

  @Override
  @Nullable
  public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
    ScheduledExecutorService executor = getScheduledExecutor();
    try {
      ErrorHandler errorHandler = this.errorHandler;
      if (errorHandler == null) {
        errorHandler = TaskUtils.getDefaultErrorHandler(true);
      }
      return new ReschedulingRunnable(task, trigger, this.clock, executor, errorHandler).schedule();
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
    ScheduledExecutorService executor = getScheduledExecutor();
    Duration delay = Duration.between(this.clock.instant(), startTime);
    try {
      return executor.schedule(errorHandlingTask(task, false), NANO.convert(delay), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
    ScheduledExecutorService executor = getScheduledExecutor();
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      return executor.scheduleAtFixedRate(errorHandlingTask(task, true),
              NANO.convert(initialDelay), NANO.convert(period), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
    ScheduledExecutorService executor = getScheduledExecutor();
    try {
      return executor.scheduleAtFixedRate(errorHandlingTask(task, true),
              0, NANO.convert(period), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
    ScheduledExecutorService executor = getScheduledExecutor();
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      return executor.scheduleWithFixedDelay(errorHandlingTask(task, true),
              NANO.convert(initialDelay), NANO.convert(delay), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
    ScheduledExecutorService executor = getScheduledExecutor();
    try {
      return executor.scheduleWithFixedDelay(errorHandlingTask(task, true),
              0, NANO.convert(delay), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(executor, task, ex);
    }
  }

  private <V> RunnableScheduledFuture<V> decorateTaskIfNecessary(RunnableScheduledFuture<V> future) {
    return taskDecorator != null ? new DelegatingRunnableScheduledFuture<>(future, taskDecorator) : future;
  }

  private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
    return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
  }

  private static class DelegatingRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {

    private final RunnableScheduledFuture<V> future;

    private final Runnable decoratedRunnable;

    public DelegatingRunnableScheduledFuture(RunnableScheduledFuture<V> future, TaskDecorator taskDecorator) {
      this.future = future;
      this.decoratedRunnable = taskDecorator.decorate(this.future);
    }

    @Override
    public void run() {
      this.decoratedRunnable.run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return this.future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
      return this.future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
      return this.future.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return this.future.get(timeout, unit);
    }

    @Override
    public boolean isPeriodic() {
      return this.future.isPeriodic();
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return this.future.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
      return this.future.compareTo(o);
    }

  }

}
