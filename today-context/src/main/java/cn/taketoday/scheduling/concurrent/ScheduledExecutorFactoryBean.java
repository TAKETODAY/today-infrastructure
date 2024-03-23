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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.support.DelegatingErrorHandlingRunnable;
import cn.taketoday.scheduling.support.TaskUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link cn.taketoday.beans.factory.FactoryBean} that sets up
 * a {@link ScheduledExecutorService}
 * (by default: a {@link ScheduledThreadPoolExecutor})
 * and exposes it for bean references.
 *
 * <p>Allows for registration of {@link ScheduledExecutorTask ScheduledExecutorTasks},
 * automatically starting the {@link ScheduledExecutorService} on initialization and
 * cancelling it on destruction of the context. In scenarios that only require static
 * registration of tasks at startup, there is no need to access the
 * {@link ScheduledExecutorService} instance itself in application code at all;
 * {@code ScheduledExecutorFactoryBean} is then just being used for lifecycle integration.
 *
 * <p>For an alternative, you may set up a {@link ScheduledThreadPoolExecutor} instance
 * directly using constructor injection, or use a factory method definition that points
 * to the {@link Executors} class.
 * <b>This is strongly recommended in particular for common {@code @Component} methods in
 * configuration classes, where this {@code FactoryBean} variant would force you to
 * return the {@code FactoryBean} type instead of {@code ScheduledExecutorService}.</b>
 *
 * <p>Note that {@link ScheduledExecutorService}
 * uses a {@link Runnable} instance that is shared between repeated executions,
 * in contrast to Quartz which instantiates a new Job for each execution.
 *
 * <p><b>WARNING:</b> {@link Runnable Runnables} submitted via a native
 * {@link ScheduledExecutorService} are removed from
 * the execution schedule once they throw an exception. If you would prefer
 * to continue execution after such an exception, switch this FactoryBean's
 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
 * property to "true".
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setThreadFactory
 * @see ScheduledExecutorTask
 * @see ScheduledExecutorService
 * @see ScheduledThreadPoolExecutor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ScheduledExecutorFactoryBean extends ExecutorConfigurationSupport implements FactoryBean<ScheduledExecutorService> {

  private int poolSize = 1;

  @Nullable
  private ScheduledExecutorTask[] scheduledExecutorTasks;

  private boolean removeOnCancelPolicy = false;

  private boolean continueScheduledExecutionAfterException = false;

  private boolean exposeUnconfigurableExecutor = false;

  @Nullable
  private ScheduledExecutorService exposedExecutor;

  /**
   * Set the ScheduledExecutorService's pool size.
   * Default is 1.
   */
  public void setPoolSize(int poolSize) {
    Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
    this.poolSize = poolSize;
  }

  /**
   * Register a list of ScheduledExecutorTask objects with the ScheduledExecutorService
   * that this FactoryBean creates. Depending on each ScheduledExecutorTask's settings,
   * it will be registered via one of ScheduledExecutorService's schedule methods.
   *
   * @see ScheduledExecutorService#schedule(Runnable, long, java.util.concurrent.TimeUnit)
   * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, java.util.concurrent.TimeUnit)
   * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, java.util.concurrent.TimeUnit)
   */
  public void setScheduledExecutorTasks(ScheduledExecutorTask... scheduledExecutorTasks) {
    this.scheduledExecutorTasks = scheduledExecutorTasks;
  }

  /**
   * Set the remove-on-cancel mode on {@link ScheduledThreadPoolExecutor}.
   * <p>Default is {@code false}. If set to {@code true}, the target executor will be
   * switched into remove-on-cancel mode (if possible, with a soft fallback otherwise).
   */
  public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
    this.removeOnCancelPolicy = removeOnCancelPolicy;
  }

  /**
   * Specify whether to continue the execution of a scheduled task
   * after it threw an exception.
   * <p>Default is "false", matching the native behavior of a
   * {@link ScheduledExecutorService}.
   * Switch this flag to "true" for exception-proof execution of each task,
   * continuing scheduled execution as in the case of successful execution.
   *
   * @see ScheduledExecutorService#scheduleAtFixedRate
   */
  public void setContinueScheduledExecutionAfterException(boolean continueScheduledExecutionAfterException) {
    this.continueScheduledExecutionAfterException = continueScheduledExecutionAfterException;
  }

  /**
   * Specify whether this FactoryBean should expose an unconfigurable
   * decorator for the created executor.
   * <p>Default is "false", exposing the raw executor as bean reference.
   * Switch this flag to "true" to strictly prevent clients from
   * modifying the executor's configuration.
   *
   * @see Executors#unconfigurableScheduledExecutorService
   */
  public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
    this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
  }

  @Override
  protected ExecutorService initializeExecutor(
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {

    ScheduledExecutorService executor =
            createExecutor(this.poolSize, threadFactory, rejectedHandler);

    if (this.removeOnCancelPolicy) {
      if (executor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
        threadPoolExecutor.setRemoveOnCancelPolicy(true);
      }
      else {
        logger.debug("Could not apply remove-on-cancel policy - not a ScheduledThreadPoolExecutor");
      }
    }

    // Register specified ScheduledExecutorTasks, if necessary.
    if (ObjectUtils.isNotEmpty(this.scheduledExecutorTasks)) {
      registerTasks(this.scheduledExecutorTasks, executor);
    }

    // Wrap executor with an unconfigurable decorator.
    this.exposedExecutor = this.exposeUnconfigurableExecutor
            ? Executors.unconfigurableScheduledExecutorService(executor) : executor;

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
   * @see ScheduledThreadPoolExecutor
   */
  protected ScheduledExecutorService createExecutor(int poolSize,
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {

    return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedHandler) {
      @Override
      protected void beforeExecute(Thread thread, Runnable task) {
        ScheduledExecutorFactoryBean.this.beforeExecute(thread, task);
      }

      @Override
      protected void afterExecute(Runnable task, Throwable ex) {
        ScheduledExecutorFactoryBean.this.afterExecute(task, ex);
      }
    };
  }

  /**
   * Register the specified {@link ScheduledExecutorTask ScheduledExecutorTasks}
   * on the given {@link ScheduledExecutorService}.
   *
   * @param tasks the specified ScheduledExecutorTasks (never empty)
   * @param executor the ScheduledExecutorService to register the tasks on.
   */
  protected void registerTasks(ScheduledExecutorTask[] tasks, ScheduledExecutorService executor) {
    for (ScheduledExecutorTask task : tasks) {
      Runnable runnable = getRunnableToSchedule(task);
      if (task.isOneTimeTask()) {
        executor.schedule(runnable, task.getDelay(), task.getTimeUnit());
      }
      else {
        if (task.isFixedRate()) {
          executor.scheduleAtFixedRate(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
        }
        else {
          executor.scheduleWithFixedDelay(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
        }
      }
    }
  }

  /**
   * Determine the actual Runnable to schedule for the given task.
   * <p>Wraps the task's Runnable in a
   * {@link cn.taketoday.scheduling.support.DelegatingErrorHandlingRunnable}
   * that will catch and log the Exception. If necessary, it will suppress the
   * Exception according to the
   * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
   * flag.
   *
   * @param task the ScheduledExecutorTask to schedule
   * @return the actual Runnable to schedule (may be a decorator)
   */
  protected Runnable getRunnableToSchedule(ScheduledExecutorTask task) {
    return this.continueScheduledExecutionAfterException
            ? new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER)
            : new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER);
  }

  @Override
  @Nullable
  public ScheduledExecutorService getObject() {
    return this.exposedExecutor;
  }

  @Override
  public Class<? extends ScheduledExecutorService> getObjectType() {
    return this.exposedExecutor != null ? this.exposedExecutor.getClass() : ScheduledExecutorService.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
