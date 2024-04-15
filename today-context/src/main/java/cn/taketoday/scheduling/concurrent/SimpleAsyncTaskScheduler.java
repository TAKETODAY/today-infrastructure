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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskRejectedException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.support.DelegatingErrorHandlingRunnable;
import cn.taketoday.scheduling.support.TaskUtils;
import cn.taketoday.util.ErrorHandler;
import cn.taketoday.util.concurrent.Future;

/**
 * A simple implementation of Infra {@link TaskScheduler} interface, using
 * a single scheduler thread and executing every scheduled task in an individual
 * separate thread. This is an attractive choice with virtual threads on JDK 21,
 * expecting common usage with {@link #setVirtualThreads setVirtualThreads(true)}.
 *
 * <p><b>NOTE: Scheduling with a fixed delay enforces execution on the single
 * scheduler thread, in order to provide traditional fixed-delay semantics!</b>
 * Prefer the use of fixed rates or cron triggers instead which are a better fit
 * with this thread-per-task scheduler variant.
 *
 * <p>Supports a graceful shutdown through {@link #setTaskTerminationTimeout},
 * at the expense of task tracking overhead per execution thread at runtime.
 * Supports limiting concurrent threads through {@link #setConcurrencyLimit}.
 * By default, the number of concurrent task executions is unlimited.
 * This allows for dynamic concurrency of scheduled task executions, in contrast
 * to {@link ThreadPoolTaskScheduler} which requires a fixed pool size.
 *
 * <p><b>NOTE: This implementation does not reuse threads!</b> Consider a
 * thread-pooling TaskScheduler implementation instead, in particular for
 * scheduling a large number of short-lived tasks. Alternatively, on JDK 21,
 * consider setting {@link #setVirtualThreads} to {@code true}.
 *
 * <p>Extends {@link SimpleAsyncTaskExecutor} and can serve as a fully capable
 * replacement for it, e.g. as a single shared instance serving as a
 * {@link cn.taketoday.core.task.TaskExecutor} as well as a {@link TaskScheduler}.
 * This is generally not the case with other executor/scheduler implementations
 * which tend to have specific constraints for the scheduler thread pool,
 * requiring a separate thread pool for general executor purposes in practice.
 *
 * <p><b>NOTE: This scheduler variant does not track the actual completion of tasks
 * but rather just the hand-off to an execution thread.</b> As a consequence,
 * a {@link ScheduledFuture} handle (e.g. from {@link #schedule(Runnable, Instant)})
 * represents that hand-off rather than the actual completion of the provided task
 * (or series of repeated tasks).
 *
 * <p>As an alternative to the built-in thread-per-task capability, this scheduler
 * can also be configured with a separate target executor for scheduled task
 * execution through {@link #setTargetTaskExecutor}: e.g. pointing to a shared
 * {@link ThreadPoolTaskExecutor} bean. This is still rather different from a
 * {@link ThreadPoolTaskScheduler} setup since it always uses a single scheduler
 * thread while dynamically dispatching to the target thread pool which may have
 * a dynamic core/max pool size range, participating in a shared concurrency limit.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setVirtualThreads
 * @see #setTaskTerminationTimeout
 * @see #setConcurrencyLimit
 * @see SimpleAsyncTaskExecutor
 * @see ThreadPoolTaskScheduler
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SimpleAsyncTaskScheduler extends SimpleAsyncTaskExecutor implements TaskScheduler,
        ApplicationContextAware, SmartLifecycle, ApplicationListener<ContextClosedEvent> {

  /**
   * The default phase for an executor {@link SmartLifecycle}: {@code Integer.MAX_VALUE / 2}.
   *
   * @see #getPhase()
   * @see ExecutorConfigurationSupport#DEFAULT_PHASE
   */
  public static final int DEFAULT_PHASE = ExecutorConfigurationSupport.DEFAULT_PHASE;

  private static final TimeUnit NANO = TimeUnit.NANOSECONDS;

  private final ScheduledExecutorService scheduledExecutor = createScheduledExecutor();

  private final ExecutorLifecycleDelegate lifecycleDelegate = new ExecutorLifecycleDelegate(this.scheduledExecutor);

  @Nullable
  private ErrorHandler errorHandler;

  private Clock clock = Clock.systemDefaultZone();

  private int phase = DEFAULT_PHASE;

  @Nullable
  private Executor targetTaskExecutor;

  @Nullable
  private ApplicationContext applicationContext;

  /**
   * Provide an {@link ErrorHandler} strategy.
   */
  public void setErrorHandler(ErrorHandler errorHandler) {
    Assert.notNull(errorHandler, "ErrorHandler is required");
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

  /**
   * Specify the lifecycle phase for pausing and resuming this executor.
   * The default is {@link #DEFAULT_PHASE}.
   *
   * @see SmartLifecycle#getPhase()
   */
  public void setPhase(int phase) {
    this.phase = phase;
  }

  /**
   * Return the lifecycle phase for pausing and resuming this executor.
   *
   * @see #setPhase
   */
  @Override
  public int getPhase() {
    return this.phase;
  }

  /**
   * Specify a custom target {@link Executor} to delegate to for
   * the individual execution of scheduled tasks. This can for example
   * be set to a separate thread pool for executing scheduled tasks,
   * whereas this scheduler keeps using its single scheduler thread.
   * <p>If not set, the regular {@link SimpleAsyncTaskExecutor}
   * arrangements kicks in with a new thread per task.
   */
  public void setTargetTaskExecutor(Executor targetTaskExecutor) {
    this.targetTaskExecutor = (targetTaskExecutor == this ? null : targetTaskExecutor);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  private ScheduledExecutorService createScheduledExecutor() {
    return new ScheduledThreadPoolExecutor(1, this::newThread) {
      @Override
      protected void beforeExecute(Thread thread, Runnable task) {
        lifecycleDelegate.beforeExecute(thread);
      }

      @Override
      protected void afterExecute(Runnable task, Throwable ex) {
        lifecycleDelegate.afterExecute();
      }
    };
  }

  @Override
  protected void doExecute(Runnable task) {
    if (this.targetTaskExecutor != null) {
      this.targetTaskExecutor.execute(task);
    }
    else {
      super.doExecute(task);
    }
  }

  private Runnable taskOnSchedulerThread(Runnable task) {
    return new DelegatingErrorHandlingRunnable(task,
            (this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true)));
  }

  private Runnable scheduledTask(Runnable task) {
    return () -> execute(new DelegatingErrorHandlingRunnable(task, this::shutdownAwareErrorHandler));
  }

  private void shutdownAwareErrorHandler(Throwable ex) {
    if (this.errorHandler != null) {
      this.errorHandler.handleError(ex);
    }
    else if (this.scheduledExecutor.isTerminated()) {
      LoggerFactory.getLogger(getClass()).debug("Ignoring scheduled task exception after shutdown", ex);
    }
    else {
      TaskUtils.getDefaultErrorHandler(true).handleError(ex);
    }
  }

  @Override
  public void execute(Runnable task) {
    super.execute(TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, false));
  }

  @Override
  public Future<Void> submit(Runnable task) {
    return super.submit(TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, false));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return super.submit(new DelegatingErrorHandlingCallable<>(task, this.errorHandler));
  }

  @Override
  @Nullable
  public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
    try {
      Runnable delegate = scheduledTask(task);
      ErrorHandler errorHandler =
              (this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
      return new ReschedulingRunnable(
              delegate, trigger, this.clock, this.scheduledExecutor, errorHandler).schedule();
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(this.scheduledExecutor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
    Duration delay = Duration.between(this.clock.instant(), startTime);
    try {
      return this.scheduledExecutor.schedule(scheduledTask(task), NANO.convert(delay), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(this.scheduledExecutor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      return this.scheduledExecutor.scheduleAtFixedRate(scheduledTask(task),
              NANO.convert(initialDelay), NANO.convert(period), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(this.scheduledExecutor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
    try {
      return this.scheduledExecutor.scheduleAtFixedRate(scheduledTask(task),
              0, NANO.convert(period), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(this.scheduledExecutor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      // Blocking task on scheduler thread for fixed delay semantics
      return this.scheduledExecutor.scheduleWithFixedDelay(taskOnSchedulerThread(task),
              NANO.convert(initialDelay), NANO.convert(delay), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(this.scheduledExecutor, task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
    try {
      // Blocking task on scheduler thread for fixed delay semantics
      return this.scheduledExecutor.scheduleWithFixedDelay(taskOnSchedulerThread(task),
              0, NANO.convert(delay), NANO);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(this.scheduledExecutor, task, ex);
    }
  }

  @Override
  public void start() {
    this.lifecycleDelegate.start();
  }

  @Override
  public void stop() {
    this.lifecycleDelegate.stop();
  }

  @Override
  public void stop(Runnable callback) {
    this.lifecycleDelegate.stop(callback);
  }

  @Override
  public boolean isRunning() {
    return this.lifecycleDelegate.isRunning();
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    if (event.getApplicationContext() == this.applicationContext) {
      this.scheduledExecutor.shutdown();
    }
  }

  @Override
  public void close() {
    for (Runnable remainingTask : this.scheduledExecutor.shutdownNow()) {
      if (remainingTask instanceof java.util.concurrent.Future<?> future) {
        future.cancel(true);
      }
    }
    super.close();
  }

}
