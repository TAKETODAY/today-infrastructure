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
package cn.taketoday.beans.factory.support;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;

/**
 * A {@link FactoryBean} that builds and exposes a preconfigured {@link ForkJoinPool}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 14:48
 */
public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

  private boolean commonPool = false;

  private int parallelism = Runtime.getRuntime().availableProcessors();

  private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

  @Nullable
  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

  private boolean asyncMode = false;

  private int awaitTerminationSeconds = 0;

  @Nullable
  private ForkJoinPool forkJoinPool;

  /**
   * Set whether to expose JDK 8's 'common' {@link ForkJoinPool}.
   * <p>Default is "false", creating a local {@link ForkJoinPool} instance based on the
   * {@link #setParallelism "parallelism"}, {@link #setThreadFactory "threadFactory"},
   * {@link #setUncaughtExceptionHandler "uncaughtExceptionHandler"} and
   * {@link #setAsyncMode "asyncMode"} properties on this FactoryBean.
   * <p><b>NOTE:</b> Setting this flag to "true" effectively ignores all other
   * properties on this FactoryBean, reusing the shared common JDK {@link ForkJoinPool}
   * instead. This is a fine choice on JDK 8 but does remove the application's ability
   * to customize ForkJoinPool behavior, in particular the use of custom threads.
   *
   * @see java.util.concurrent.ForkJoinPool#commonPool()
   */
  public void setCommonPool(boolean commonPool) {
    this.commonPool = commonPool;
  }

  /**
   * Specify the parallelism level. Default is {@link Runtime#availableProcessors()}.
   */
  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  /**
   * Set the factory for creating new ForkJoinWorkerThreads.
   * Default is {@link ForkJoinPool#defaultForkJoinWorkerThreadFactory}.
   */
  public void setThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  /**
   * Set the handler for internal worker threads that terminate due to unrecoverable errors
   * encountered while executing tasks. Default is none.
   */
  public void setUncaughtExceptionHandler(@Nullable Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
    this.uncaughtExceptionHandler = uncaughtExceptionHandler;
  }

  /**
   * Specify whether to establish a local first-in-first-out scheduling mode for forked tasks
   * that are never joined. This mode (asyncMode = {@code true}) may be more appropriate
   * than the default locally stack-based mode in applications in which worker threads only
   * process event-style asynchronous tasks. Default is {@code false}.
   */
  public void setAsyncMode(boolean asyncMode) {
    this.asyncMode = asyncMode;
  }

  /**
   * Set the maximum number of seconds that this ForkJoinPool is supposed to block
   * on shutdown in order to wait for remaining tasks to complete their execution
   * before the rest of the container continues to shut down. This is particularly
   * useful if your remaining tasks are likely to need access to other resources
   * that are also managed by the container.
   * <p>By default, this ForkJoinPool won't wait for the termination of tasks at all.
   * It will continue to fully execute all ongoing tasks as well as all remaining
   * tasks in the queue, in parallel to the rest of the container shutting down.
   * In contrast, if you specify an await-termination period using this property,
   * this executor will wait for the given time (max) for the termination of tasks.
   * <p>Note that this feature works for the {@link #setCommonPool "commonPool"}
   * mode as well. The underlying ForkJoinPool won't actually terminate in that
   * case but will wait for all tasks to terminate.
   *
   * @see java.util.concurrent.ForkJoinPool#shutdown()
   * @see java.util.concurrent.ForkJoinPool#awaitTermination
   */
  public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
    this.awaitTerminationSeconds = awaitTerminationSeconds;
  }

  @Override
  public void afterPropertiesSet() {
    this.forkJoinPool = this.commonPool
                        ? ForkJoinPool.commonPool()
                        : new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode);
  }

  @Override
  @Nullable
  public ForkJoinPool getObject() {
    return this.forkJoinPool;
  }

  @Override
  public Class<?> getObjectType() {
    return ForkJoinPool.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() {
    if (this.forkJoinPool != null) {
      // Ignored for the common pool.
      this.forkJoinPool.shutdown();

      // Wait for all tasks to terminate - works for the common pool as well.
      if (this.awaitTerminationSeconds > 0) {
        try {
          this.forkJoinPool.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

}
