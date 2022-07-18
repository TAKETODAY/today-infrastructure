/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.scheduling.concurrent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.task.TaskRejectedException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.TriggerContext;
import cn.taketoday.scheduling.support.SimpleTriggerContext;
import cn.taketoday.scheduling.support.TaskUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ErrorHandler;
import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;

/**
 * Adapter that takes a {@code java.util.concurrent.ScheduledExecutorService} and
 * exposes a Framework {@link TaskScheduler} for it.
 * Extends {@link ConcurrentTaskExecutor} in order to implement the
 * {@link cn.taketoday.scheduling.SchedulingTaskExecutor} interface as well.
 *
 * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorService}
 * in order to use it for trigger-based scheduling if possible, instead of
 * local trigger management which ends up delegating to regular delay-based scheduling
 * against the {@code java.util.concurrent.ScheduledExecutorService} API. For JSR-236 style
 * lookup in a Jakarta EE environment, consider using {@link DefaultManagedTaskScheduler}.
 *
 * <p>Note that there is a pre-built {@link ThreadPoolTaskScheduler} that allows for
 * defining a {@link java.util.concurrent.ScheduledThreadPoolExecutor} in bean style,
 * exposing it as a Framework {@link TaskScheduler} directly.
 * This is a convenient alternative to a raw ScheduledThreadPoolExecutor definition with
 * a separate definition of the present adapter class.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see Executors
 * @see DefaultManagedTaskScheduler
 * @see ThreadPoolTaskScheduler
 * @since 4.0
 */
public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

  @Nullable
  private static final Class<?> managedScheduledExecutorServiceClass = ClassUtils.load(
          "jakarta.enterprise.concurrent.ManagedScheduledExecutorService",
          ConcurrentTaskScheduler.class.getClassLoader()
  );

  private ScheduledExecutorService scheduledExecutor;

  private boolean enterpriseConcurrentScheduler = false;

  @Nullable
  private ErrorHandler errorHandler;

  private Clock clock = Clock.systemDefaultZone();

  /**
   * Create a new ConcurrentTaskScheduler,
   * using a single thread executor as default.
   *
   * @see java.util.concurrent.Executors#newSingleThreadScheduledExecutor()
   */
  public ConcurrentTaskScheduler() {
    super();
    this.scheduledExecutor = initScheduledExecutor(null);
  }

  /**
   * Create a new ConcurrentTaskScheduler, using the given
   * {@link ScheduledExecutorService} as shared delegate.
   * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorService}
   * in order to use it for trigger-based scheduling if possible,
   * instead of local trigger management.
   *
   * @param scheduledExecutor the {@link ScheduledExecutorService}
   * to delegate to for {@link cn.taketoday.scheduling.SchedulingTaskExecutor}
   * as well as {@link TaskScheduler} invocations
   */
  public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
    super(scheduledExecutor);
    this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
  }

  /**
   * Create a new ConcurrentTaskScheduler, using the given {@link Executor}
   * and {@link ScheduledExecutorService} as delegates.
   * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorService}
   * in order to use it for trigger-based scheduling if possible,
   * instead of local trigger management.
   *
   * @param concurrentExecutor the {@link Executor} to delegate to
   * for {@link cn.taketoday.scheduling.SchedulingTaskExecutor} invocations
   * @param scheduledExecutor the {@link ScheduledExecutorService}
   * to delegate to for {@link TaskScheduler} invocations
   */
  public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
    super(concurrentExecutor);
    this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
  }

  private ScheduledExecutorService initScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
    if (scheduledExecutor != null) {
      this.scheduledExecutor = scheduledExecutor;
      this.enterpriseConcurrentScheduler = (managedScheduledExecutorServiceClass != null &&
              managedScheduledExecutorServiceClass.isInstance(scheduledExecutor));
    }
    else {
      this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
      this.enterpriseConcurrentScheduler = false;
    }
    return this.scheduledExecutor;
  }

  /**
   * Specify the {@link ScheduledExecutorService} to delegate to.
   * <p>Autodetects a JSR-236 {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorService}
   * in order to use it for trigger-based scheduling if possible,
   * instead of  local trigger management.
   * <p>Note: This will only apply to {@link TaskScheduler} invocations.
   * If you want the given executor to apply to
   * {@link cn.taketoday.scheduling.SchedulingTaskExecutor} invocations
   * as well, pass the same executor reference to {@link #setConcurrentExecutor}.
   *
   * @see #setConcurrentExecutor
   */
  public void setScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
    initScheduledExecutor(scheduledExecutor);
  }

  /**
   * Provide an {@link ErrorHandler} strategy.
   */
  public void setErrorHandler(ErrorHandler errorHandler) {
    Assert.notNull(errorHandler, "ErrorHandler must not be null");
    this.errorHandler = errorHandler;
  }

  /**
   * Set the clock to use for scheduling purposes.
   * <p>The default clock is the system clock for the default time zone.
   *
   * @see Clock#systemDefaultZone()
   */
  public void setClock(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Clock getClock() {
    return this.clock;
  }

  @Override
  @Nullable
  public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
    try {
      if (this.enterpriseConcurrentScheduler) {
        return new EnterpriseConcurrentTriggerScheduler().schedule(decorateTask(task, true), trigger);
      }
      else {
        ErrorHandler errorHandler = this.errorHandler;
        if (errorHandler == null) {
          errorHandler = TaskUtils.getDefaultErrorHandler(true);
        }
        return new ReschedulingRunnable(task, trigger, this.clock, this.scheduledExecutor, errorHandler).schedule();
      }
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      return scheduledExecutor.schedule(decorateTask(task, false),
              initialDelay.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      return scheduledExecutor.scheduleAtFixedRate(
              decorateTask(task, true),
              initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
    try {
      return scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true),
              0, period.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
    Duration initialDelay = Duration.between(this.clock.instant(), startTime);
    try {
      return scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true),
              initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
    try {
      return scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true),
              0, delay.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
    }
  }

  private Runnable decorateTask(Runnable task, boolean isRepeatingTask) {
    Runnable result = TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
    if (this.enterpriseConcurrentScheduler) {
      result = ManagedTaskBuilder.buildManagedTask(result, task.toString());
    }
    return result;
  }

  /**
   * Delegate that adapts a Spring Trigger to a JSR-236 Trigger.
   * Separated into an inner class in order to avoid a hard dependency on the JSR-236 API.
   */
  private class EnterpriseConcurrentTriggerScheduler {

    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
      ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) scheduledExecutor;
      return executor.schedule(task, new TriggerAdapter(trigger));
    }

    private static class TriggerAdapter implements jakarta.enterprise.concurrent.Trigger {

      private final Trigger adaptee;

      public TriggerAdapter(Trigger adaptee) {
        this.adaptee = adaptee;
      }

      @Override
      @Nullable
      public Date getNextRunTime(@Nullable LastExecution le, Date taskScheduledTime) {
        Instant instant = adaptee.nextExecution(new LastExecutionAdapter(le));
        return instant != null ? Date.from(instant) : null;
      }

      @Nullable
      private static Instant toInstant(@Nullable Date date) {
        return date != null ? date.toInstant() : null;
      }

      @Override
      public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
        return false;
      }

      private static class LastExecutionAdapter implements TriggerContext {

        @Nullable
        private final LastExecution le;

        public LastExecutionAdapter(@Nullable LastExecution le) {
          this.le = le;
        }

        @Override
        public Instant lastScheduledExecution() {
          return (this.le != null) ? toInstant(this.le.getScheduledStart()) : null;
        }

        @Override
        public Instant lastActualExecution() {
          return (this.le != null) ? toInstant(this.le.getRunStart()) : null;
        }

        @Override
        public Instant lastCompletion() {
          return (this.le != null) ? toInstant(this.le.getRunEnd()) : null;
        }
      }
    }

  }

}
