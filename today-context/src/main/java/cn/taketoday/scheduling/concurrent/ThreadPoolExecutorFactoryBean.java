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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Nullable;

/**
 * JavaBean that allows for configuring a {@link ThreadPoolExecutor}
 * in bean style (through its "corePoolSize", "maxPoolSize", "keepAliveSeconds",
 * "queueCapacity" properties) and exposing it as a bean reference of its native
 * {@link ExecutorService} type.
 *
 * <p>The default configuration is a core pool size of 1, with unlimited max pool size
 * and unlimited queue capacity. This is roughly equivalent to
 * {@link Executors#newSingleThreadExecutor()}, sharing a single
 * thread for all tasks. Setting {@link #setQueueCapacity "queueCapacity"} to 0 mimics
 * {@link Executors#newCachedThreadPool()}, with immediate scaling
 * of threads in the pool to a potentially very high number. Consider also setting a
 * {@link #setMaxPoolSize "maxPoolSize"} at that point, as well as possibly a higher
 * {@link #setCorePoolSize "corePoolSize"} (see also the
 * {@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"} mode of scaling).
 *
 * <p>For an alternative, you may set up a {@link ThreadPoolExecutor} instance directly
 * using constructor injection, or use a factory method definition that points to the
 * {@link Executors} class.
 * <b>This is strongly recommended in particular for common {@code @Component} methods in
 * configuration classes, where this {@code FactoryBean} variant would force you to
 * return the {@code FactoryBean} type instead of the actual {@code Executor} type.</b>
 *
 * <p>If you need a timing-based {@link java.util.concurrent.ScheduledExecutorService}
 * instead, consider {@link ScheduledExecutorFactoryBean}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ExecutorService
 * @see Executors
 * @see ThreadPoolExecutor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ThreadPoolExecutorFactoryBean extends ExecutorConfigurationSupport
        implements FactoryBean<ExecutorService> {

  private int corePoolSize = 1;

  private int maxPoolSize = Integer.MAX_VALUE;

  private int keepAliveSeconds = 60;

  private int queueCapacity = Integer.MAX_VALUE;

  private boolean allowCoreThreadTimeOut = false;

  private boolean prestartAllCoreThreads = false;

  private boolean strictEarlyShutdown = false;

  private boolean exposeUnconfigurableExecutor = false;

  @Nullable
  private ExecutorService exposedExecutor;

  /**
   * Set the ThreadPoolExecutor's core pool size.
   * Default is 1.
   */
  public void setCorePoolSize(int corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  /**
   * Set the ThreadPoolExecutor's maximum pool size.
   * Default is {@code Integer.MAX_VALUE}.
   */
  public void setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  /**
   * Set the ThreadPoolExecutor's keep-alive seconds.
   * Default is 60.
   */
  public void setKeepAliveSeconds(int keepAliveSeconds) {
    this.keepAliveSeconds = keepAliveSeconds;
  }

  /**
   * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
   * Default is {@code Integer.MAX_VALUE}.
   * <p>Any positive value will lead to a LinkedBlockingQueue instance;
   * any other value will lead to a SynchronousQueue instance.
   *
   * @see java.util.concurrent.LinkedBlockingQueue
   * @see java.util.concurrent.SynchronousQueue
   */
  public void setQueueCapacity(int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

  /**
   * Specify whether to allow core threads to time out. This enables dynamic
   * growing and shrinking even in combination with a non-zero queue (since
   * the max pool size will only grow once the queue is full).
   * <p>Default is "false".
   *
   * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
   */
  public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
    this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
  }

  /**
   * Specify whether to start all core threads, causing them to idly wait for work.
   * <p>Default is "false".
   *
   * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
   */
  public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
    this.prestartAllCoreThreads = prestartAllCoreThreads;
  }

  /**
   * Specify whether to initiate an early shutdown signal on context close,
   * disposing all idle threads and rejecting further task submissions.
   * <p>Default is "false".
   * See {@link ThreadPoolTaskExecutor#setStrictEarlyShutdown} for details.
   *
   * @see #initiateShutdown()
   */
  public void setStrictEarlyShutdown(boolean defaultEarlyShutdown) {
    this.strictEarlyShutdown = defaultEarlyShutdown;
  }

  /**
   * Specify whether this FactoryBean should expose an unconfigurable
   * decorator for the created executor.
   * <p>Default is "false", exposing the raw executor as bean reference.
   * Switch this flag to "true" to strictly prevent clients from
   * modifying the executor's configuration.
   *
   * @see java.util.concurrent.Executors#unconfigurableExecutorService
   */
  public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
    this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
  }

  @Override
  protected ExecutorService initializeExecutor(
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {

    BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
    ThreadPoolExecutor executor = createExecutor(this.corePoolSize, this.maxPoolSize,
            this.keepAliveSeconds, queue, threadFactory, rejectedHandler);
    if (this.allowCoreThreadTimeOut) {
      executor.allowCoreThreadTimeOut(true);
    }
    if (this.prestartAllCoreThreads) {
      executor.prestartAllCoreThreads();
    }

    // Wrap executor with an unconfigurable decorator.
    this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
            Executors.unconfigurableExecutorService(executor) : executor);

    return executor;
  }

  /**
   * Create a new instance of {@link ThreadPoolExecutor} or a subclass thereof.
   * <p>The default implementation creates a standard {@link ThreadPoolExecutor}.
   * Can be overridden to provide custom {@link ThreadPoolExecutor} subclasses.
   *
   * @param corePoolSize the specified core pool size
   * @param maxPoolSize the specified maximum pool size
   * @param keepAliveSeconds the specified keep-alive time in seconds
   * @param queue the BlockingQueue to use
   * @param threadFactory the ThreadFactory to use
   * @param rejectedExecutionHandler the RejectedExecutionHandler to use
   * @return a new ThreadPoolExecutor instance
   * @see #afterPropertiesSet()
   */
  protected ThreadPoolExecutor createExecutor(
          int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

    return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
            keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler) {
      @Override
      protected void beforeExecute(Thread thread, Runnable task) {
        ThreadPoolExecutorFactoryBean.this.beforeExecute(thread, task);
      }

      @Override
      protected void afterExecute(Runnable task, Throwable ex) {
        ThreadPoolExecutorFactoryBean.this.afterExecute(task, ex);
      }
    };
  }

  /**
   * Create the BlockingQueue to use for the ThreadPoolExecutor.
   * <p>A LinkedBlockingQueue instance will be created for a positive
   * capacity value; a SynchronousQueue else.
   *
   * @param queueCapacity the specified queue capacity
   * @return the BlockingQueue instance
   * @see java.util.concurrent.LinkedBlockingQueue
   * @see java.util.concurrent.SynchronousQueue
   */
  protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
    if (queueCapacity > 0) {
      return new LinkedBlockingQueue<>(queueCapacity);
    }
    else {
      return new SynchronousQueue<>();
    }
  }

  @Override
  protected void initiateEarlyShutdown() {
    if (this.strictEarlyShutdown) {
      super.initiateEarlyShutdown();
    }
  }

  @Override
  @Nullable
  public ExecutorService getObject() {
    return this.exposedExecutor;
  }

  @Override
  public Class<? extends ExecutorService> getObjectType() {
    return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ExecutorService.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
