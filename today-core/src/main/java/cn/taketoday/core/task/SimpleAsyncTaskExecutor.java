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
package cn.taketoday.core.task;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrencyThrottleSupport;
import cn.taketoday.util.CustomizableThreadCreator;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.util.concurrent.ListenableFutureTask;

/**
 * {@link TaskExecutor} implementation that fires up a new Thread for each task,
 * executing it asynchronously.
 *
 * <p>Supports limiting concurrent threads through the "concurrencyLimit"
 * bean property. By default, the number of concurrent threads is unlimited.
 *
 * <p><b>NOTE: This implementation does not reuse threads!</b> Consider a
 * thread-pooling TaskExecutor implementation instead, in particular for
 * executing a large number of short-lived tasks.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setConcurrencyLimit
 * @see SyncTaskExecutor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator
        implements AsyncListenableTaskExecutor, Serializable {

  /**
   * Permit any number of concurrent invocations: that is, don't throttle concurrency.
   *
   * @see ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
   */
  public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

  /**
   * Switch concurrency 'off': that is, don't allow any concurrent invocations.
   *
   * @see ConcurrencyThrottleSupport#NO_CONCURRENCY
   */
  public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;

  /** Internal concurrency throttle used by this executor. */
  private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

  @Nullable
  private ThreadFactory threadFactory;

  @Nullable
  private TaskDecorator taskDecorator;

  /**
   * Create a new SimpleAsyncTaskExecutor with default thread name prefix.
   */
  public SimpleAsyncTaskExecutor() {
    super();
  }

  /**
   * Create a new SimpleAsyncTaskExecutor with the given thread name prefix.
   *
   * @param threadNamePrefix the prefix to use for the names of newly created threads
   */
  public SimpleAsyncTaskExecutor(String threadNamePrefix) {
    super(threadNamePrefix);
  }

  /**
   * Create a new SimpleAsyncTaskExecutor with the given external thread factory.
   *
   * @param threadFactory the factory to use for creating new Threads
   */
  public SimpleAsyncTaskExecutor(@Nullable ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  /**
   * Specify an external factory to use for creating new Threads,
   * instead of relying on the local properties of this executor.
   * <p>You may specify an inner ThreadFactory bean or also a ThreadFactory reference
   * obtained from JNDI (on a Java EE 6 server) or some other lookup mechanism.
   *
   * @see #setThreadNamePrefix
   * @see #setThreadPriority
   */
  public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  /**
   * Return the external factory to use for creating new Threads, if any.
   */
  @Nullable
  public final ThreadFactory getThreadFactory() {
    return this.threadFactory;
  }

  /**
   * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
   * about to be executed.
   * <p>Note that such a decorator is not necessarily being applied to the
   * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
   * execution callback (which may be a wrapper around the user-supplied task).
   * <p>The primary use case is to set some execution context around the task's
   * invocation, or to provide some monitoring/statistics for task execution.
   * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
   * is limited to plain {@code Runnable} execution via {@code execute} calls.
   * In case of {@code #submit} calls, the exposed {@code Runnable} will be a
   * {@code FutureTask} which does not propagate any exceptions; you might
   * have to cast it and call {@code Future#get} to evaluate exceptions.
   */
  public final void setTaskDecorator(@Nullable TaskDecorator taskDecorator) {
    this.taskDecorator = taskDecorator;
  }

  /**
   * Set the maximum number of parallel accesses allowed.
   * -1 indicates no concurrency limit at all.
   * <p>In principle, this limit can be changed at runtime,
   * although it is generally designed as a config time setting.
   * NOTE: Do not switch between -1 and any concrete limit at runtime,
   * as this will lead to inconsistent concurrency counts: A limit
   * of -1 effectively turns off concurrency counting completely.
   *
   * @see #UNBOUNDED_CONCURRENCY
   */
  public void setConcurrencyLimit(int concurrencyLimit) {
    this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
  }

  /**
   * Return the maximum number of parallel accesses allowed.
   */
  public final int getConcurrencyLimit() {
    return this.concurrencyThrottle.getConcurrencyLimit();
  }

  /**
   * Return whether this throttle is currently active.
   *
   * @return {@code true} if the concurrency limit for this instance is active
   * @see #getConcurrencyLimit()
   * @see #setConcurrencyLimit
   */
  public final boolean isThrottleActive() {
    return this.concurrencyThrottle.isThrottleActive();
  }

  /**
   * Executes the given task, within a concurrency throttle
   * if configured (through the superclass's settings).
   *
   * @see #doExecute(Runnable)
   */
  @Override
  public void execute(Runnable task) {
    execute(task, TIMEOUT_INDEFINITE);
  }

  /**
   * Executes the given task, within a concurrency throttle
   * if configured (through the superclass's settings).
   * <p>Executes urgent tasks (with 'immediate' timeout) directly,
   * bypassing the concurrency throttle (if active). All other
   * tasks are subject to throttling.
   *
   * @see #TIMEOUT_IMMEDIATE
   * @see #doExecute(Runnable)
   */
  @Override
  public void execute(Runnable task, long startTimeout) {
    Assert.notNull(task, "Runnable must not be null");
    TaskDecorator taskDecorator = this.taskDecorator;
    if (taskDecorator != null) {
      task = taskDecorator.decorate(task);
    }
    if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
      concurrencyThrottle.beforeAccess();
      doExecute(new ConcurrencyThrottlingRunnable(task));
    }
    else {
      doExecute(task);
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    FutureTask<Object> future = new FutureTask<>(task, null);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    var future = new FutureTask<>(task);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  @Override
  public ListenableFuture<?> submitListenable(Runnable task) {
    var future = new ListenableFutureTask<>(task, null);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  @Override
  public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
    var future = new ListenableFutureTask<>(task);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  /**
   * Template method for the actual execution of a task.
   * <p>The default implementation creates a new Thread and starts it.
   *
   * @param task the Runnable to execute
   * @see #setThreadFactory
   * @see #createThread
   * @see Thread#start()
   */
  protected void doExecute(Runnable task) {
    ThreadFactory threadFactory = this.threadFactory;
    if (threadFactory != null) {
      threadFactory.newThread(task).start();
    }
    else {
      createThread(task).start();
    }
  }

  /**
   * Subclass of the general ConcurrencyThrottleSupport class,
   * making {@code beforeAccess()} and {@code afterAccess()}
   * visible to the surrounding class.
   */
  private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

    @Override
    protected void beforeAccess() {
      super.beforeAccess();
    }

    @Override
    protected void afterAccess() {
      super.afterAccess();
    }
  }

  /**
   * This Runnable calls {@code afterAccess()} after the
   * target Runnable has finished its execution.
   */
  private class ConcurrencyThrottlingRunnable implements Runnable {

    private final Runnable target;

    public ConcurrencyThrottlingRunnable(Runnable target) {
      this.target = target;
    }

    @Override
    public void run() {
      try {
        this.target.run();
      }
      finally {
        concurrencyThrottle.afterAccess();
      }
    }
  }

}
