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

package cn.taketoday.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import cn.taketoday.util.concurrent.FutureUtils;

/**
 * Extended interface for asynchronous {@link TaskExecutor} implementations,
 * offering support for {@link java.util.concurrent.Callable}.
 *
 * <p>Note: The {@link java.util.concurrent.Executors} class includes a set of
 * methods that can convert some other common closure-like objects, for example,
 * {@link java.security.PrivilegedAction} to {@link Callable} before executing them.
 *
 * <p>Implementing this interface also indicates that the {@link #execute(Runnable)}
 * method will not execute its Runnable in the caller's thread but rather
 * asynchronously in some other thread.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleAsyncTaskExecutor
 * @see Callable
 * @see java.util.concurrent.Executors
 * @see cn.taketoday.scheduling.SchedulingTaskExecutor
 * @since 4.0
 */
public interface AsyncTaskExecutor extends TaskExecutor {

  /** Constant that indicates immediate execution. */
  long TIMEOUT_IMMEDIATE = 0;

  /** Constant that indicates no time limit. */
  long TIMEOUT_INDEFINITE = Long.MAX_VALUE;

  /**
   * Execute the given {@code task}.
   *
   * @param task the {@code Runnable} to execute (never {@code null})
   * @param startTimeout the time duration (milliseconds) within which the task is
   * supposed to start. This is intended as a hint to the executor, allowing for
   * preferred handling of immediate tasks. Typical values are {@link #TIMEOUT_IMMEDIATE}
   * or {@link #TIMEOUT_INDEFINITE} (the default as used by {@link #execute(Runnable)}).
   * @throws TaskTimeoutException in case of the task being rejected because
   * of the timeout (i.e. it cannot be started in time)
   * @throws TaskRejectedException if the given task was not accepted
   */
  default void execute(Runnable task, long startTimeout) {
    execute(task);
  }

  /**
   * Submit a Runnable task for execution, receiving a Future representing that task.
   * The Future will return a {@code null} result upon completion.
   * <p>this method comes with a default implementation that delegates
   * to {@link #execute(Runnable)}.
   *
   * @param task the {@code Runnable} to execute (never {@code null})
   * @return a Future representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  default Future<?> submit(Runnable task) {
    FutureTask<Object> future = new FutureTask<>(task, null);
    execute(future);
    return future;
  }

  /**
   * Submit a Callable task for execution, receiving a Future representing that task.
   * The Future will return the Callable's result upon completion.
   * <p>this method comes with a default implementation that delegates
   * to {@link #execute(Runnable)}.
   *
   * @param task the {@code Callable} to execute (never {@code null})
   * @return a Future representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  default <T> Future<T> submit(Callable<T> task) {
    FutureTask<T> future = new FutureTask<>(task);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  /**
   * Submit a {@code Runnable} task for execution, receiving a {@code CompletableFuture}
   * representing that task. The Future will return a {@code null} result upon completion.
   *
   * @param task the {@code Runnable} to execute (never {@code null})
   * @return a {@code CompletableFuture} representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  default CompletableFuture<Void> submitCompletable(Runnable task) {
    return CompletableFuture.runAsync(task, this);
  }

  /**
   * Submit a {@code Callable} task for execution, receiving a {@code CompletableFuture}
   * representing that task. The Future will return the Callable's result upon
   * completion.
   *
   * @param task the {@code Callable} to execute (never {@code null})
   * @return a {@code CompletableFuture} representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  default <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
    return FutureUtils.callAsync(task, this);
  }

}
