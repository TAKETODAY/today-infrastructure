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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for setting up a {@link ExecutorService}
 * (typically a {@link ThreadPoolExecutor} or
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}).
 * Defines common configuration settings and common lifecycle handling.
 *
 * @author Juergen Hoeller
 * @see ExecutorService
 * @see java.util.concurrent.Executors
 * @see ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
        implements BeanNameAware, InitializingBean, DisposableBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private ThreadFactory threadFactory = this;

  private boolean threadNamePrefixSet = false;

  private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

  private boolean waitForTasksToCompleteOnShutdown = false;

  private long awaitTerminationMillis = 0;

  @Nullable
  private String beanName;

  @Nullable
  private ExecutorService executor;

  /**
   * Set the ThreadFactory to use for the ExecutorService's thread pool.
   * Default is the underlying ExecutorService's default thread factory.
   * <p>In a Jakarta EE or other managed environment with JSR-236 support,
   * consider specifying a JNDI-located ManagedThreadFactory: by default,
   * to be found at "java:comp/DefaultManagedThreadFactory".
   * Use the "jee:jndi-lookup" namespace element in XML or the programmatic
   * {@link cn.taketoday.jndi.JndiLocatorDelegate} for convenient lookup.
   * Alternatively, consider using  {@link DefaultManagedAwareThreadFactory}
   * with its fallback to local threads in case of no managed thread factory found.
   *
   * @see java.util.concurrent.Executors#defaultThreadFactory()
   * @see jakarta.enterprise.concurrent.ManagedThreadFactory
   * @see DefaultManagedAwareThreadFactory
   */
  public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
    this.threadFactory = (threadFactory != null ? threadFactory : this);
  }

  @Override
  public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
    super.setThreadNamePrefix(threadNamePrefix);
    this.threadNamePrefixSet = true;
  }

  /**
   * Set the RejectedExecutionHandler to use for the ExecutorService.
   * Default is the ExecutorService's default abort policy.
   *
   * @see ThreadPoolExecutor.AbortPolicy
   */
  public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
    this.rejectedExecutionHandler =
            (rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
  }

  /**
   * Set whether to wait for scheduled tasks to complete on shutdown,
   * not interrupting running tasks and executing all tasks in the queue.
   * <p>Default is "false", shutting down immediately through interrupting
   * ongoing tasks and clearing the queue. Switch this flag to "true" if you
   * prefer fully completed tasks at the expense of a longer shutdown phase.
   * <p>Note that  container shutdown continues while ongoing tasks
   * are being completed. If you want this executor to block and wait for the
   * termination of tasks before the rest of the container continues to shut
   * down - e.g. in order to keep up other resources that your tasks may need -,
   * set the {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}
   * property instead of or in addition to this property.
   *
   * @see ExecutorService#shutdown()
   * @see ExecutorService#shutdownNow()
   */
  public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
    this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
  }

  /**
   * Set the maximum number of seconds that this executor is supposed to block
   * on shutdown in order to wait for remaining tasks to complete their execution
   * before the rest of the container continues to shut down. This is particularly
   * useful if your remaining tasks are likely to need access to other resources
   * that are also managed by the container.
   * <p>By default, this executor won't wait for the termination of tasks at all.
   * It will either shut down immediately, interrupting ongoing tasks and clearing
   * the remaining task queue - or, if the
   * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
   * flag has been set to {@code true}, it will continue to fully execute all
   * ongoing tasks as well as all remaining tasks in the queue, in parallel to
   * the rest of the container shutting down.
   * <p>In either case, if you specify an await-termination period using this property,
   * this executor will wait for the given time (max) for the termination of tasks.
   * As a rule of thumb, specify a significantly higher timeout here if you set
   * "waitForTasksToCompleteOnShutdown" to {@code true} at the same time,
   * since all remaining tasks in the queue will still get executed - in contrast
   * to the default shutdown behavior where it's just about waiting for currently
   * executing tasks that aren't reacting to thread interruption.
   *
   * @see #setAwaitTerminationMillis
   * @see ExecutorService#shutdown()
   * @see ExecutorService#awaitTermination
   */
  public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
    this.awaitTerminationMillis = awaitTerminationSeconds * 1000L;
  }

  /**
   * Variant of {@link #setAwaitTerminationSeconds} with millisecond precision.
   *
   * @see #setAwaitTerminationSeconds
   */
  public void setAwaitTerminationMillis(long awaitTerminationMillis) {
    this.awaitTerminationMillis = awaitTerminationMillis;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  /**
   * Calls {@code initialize()} after the container applied all property values.
   *
   * @see #initialize()
   */
  @Override
  public void afterPropertiesSet() {
    initialize();
  }

  /**
   * Set up the ExecutorService.
   */
  public void initialize() {
    if (logger.isDebugEnabled()) {
      logger.debug("Initializing ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
    }
    if (!this.threadNamePrefixSet && this.beanName != null) {
      setThreadNamePrefix(this.beanName + "-");
    }
    this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
  }

  /**
   * Create the target {@link ExecutorService} instance.
   * Called by {@code afterPropertiesSet}.
   *
   * @param threadFactory the ThreadFactory to use
   * @param rejectedExecutionHandler the RejectedExecutionHandler to use
   * @return a new ExecutorService instance
   * @see #afterPropertiesSet()
   */
  protected abstract ExecutorService initializeExecutor(
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);

  /**
   * Calls {@code shutdown} when the BeanFactory destroys
   * the task executor instance.
   *
   * @see #shutdown()
   */
  @Override
  public void destroy() {
    shutdown();
  }

  /**
   * Perform a shutdown on the underlying ExecutorService.
   *
   * @see ExecutorService#shutdown()
   * @see ExecutorService#shutdownNow()
   */
  public void shutdown() {
    if (logger.isDebugEnabled()) {
      logger.debug("Shutting down ExecutorService{}", this.beanName != null ? " '" + this.beanName + "'" : "");
    }
    if (this.executor != null) {
      if (this.waitForTasksToCompleteOnShutdown) {
        this.executor.shutdown();
      }
      else {
        for (Runnable remainingTask : this.executor.shutdownNow()) {
          cancelRemainingTask(remainingTask);
        }
      }
      awaitTerminationIfNecessary(this.executor);
    }
  }

  /**
   * Cancel the given remaining task which never commended execution,
   * as returned from {@link ExecutorService#shutdownNow()}.
   *
   * @param task the task to cancel (typically a {@link RunnableFuture})
   * @see #shutdown()
   * @see RunnableFuture#cancel(boolean)
   */
  protected void cancelRemainingTask(Runnable task) {
    if (task instanceof Future) {
      ((Future<?>) task).cancel(true);
    }
  }

  /**
   * Wait for the executor to terminate, according to the value of the
   * {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} property.
   */
  private void awaitTerminationIfNecessary(ExecutorService executor) {
    if (this.awaitTerminationMillis > 0) {
      try {
        if (!executor.awaitTermination(this.awaitTerminationMillis, TimeUnit.MILLISECONDS)) {
          if (logger.isWarnEnabled()) {
            logger.warn("Timed out while waiting for executor{} to terminate", this.beanName != null ? " '" + this.beanName + "'" : "");
          }
        }
      }
      catch (InterruptedException ex) {
        if (logger.isWarnEnabled()) {
          logger.warn("Interrupted while waiting for executor{} to terminate", this.beanName != null ? " '" + this.beanName + "'" : "");
        }
        Thread.currentThread().interrupt();
      }
    }
  }

}
