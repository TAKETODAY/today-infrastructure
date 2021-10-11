/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.core.task.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.core.task.AsyncListenableTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskRejectedException;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.util.concurrent.ListenableFutureTask;

/**
 * Adapter that takes a JDK {@code java.util.concurrent.Executor} and
 * exposes a {@link cn.taketoday.core.task.TaskExecutor} for it.
 * Also detects an extended {@code java.util.concurrent.ExecutorService}, adapting
 * the {@link cn.taketoday.core.task.AsyncTaskExecutor} interface accordingly.
 *
 * @author Juergen Hoeller
 * @see Executor
 * @see ExecutorService
 * @see java.util.concurrent.Executors
 * @since 4.0
 */
public class TaskExecutorAdapter implements AsyncListenableTaskExecutor {

  private final Executor concurrentExecutor;

  @Nullable
  private TaskDecorator taskDecorator;

  /**
   * Create a new TaskExecutorAdapter,
   * using the given JDK concurrent executor.
   *
   * @param concurrentExecutor
   *         the JDK concurrent executor to delegate to
   */
  public TaskExecutorAdapter(Executor concurrentExecutor) {
    Assert.notNull(concurrentExecutor, "Executor must not be null");
    this.concurrentExecutor = concurrentExecutor;
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
   * Delegates to the specified JDK concurrent executor.
   *
   * @see Executor#execute(Runnable)
   */
  @Override
  public void execute(Runnable task) {
    try {
      doExecute(this.concurrentExecutor, this.taskDecorator, task);
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public void execute(Runnable task, long startTimeout) {
    execute(task);
  }

  @Override
  public Future<?> submit(Runnable task) {
    try {
      if (this.taskDecorator == null && this.concurrentExecutor instanceof ExecutorService) {
        return ((ExecutorService) this.concurrentExecutor).submit(task);
      }
      else {
        FutureTask<Object> future = new FutureTask<>(task, null);
        doExecute(this.concurrentExecutor, this.taskDecorator, future);
        return future;
      }
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    try {
      if (this.taskDecorator == null && this.concurrentExecutor instanceof ExecutorService) {
        return ((ExecutorService) this.concurrentExecutor).submit(task);
      }
      else {
        FutureTask<T> future = new FutureTask<>(task);
        doExecute(this.concurrentExecutor, this.taskDecorator, future);
        return future;
      }
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public ListenableFuture<?> submitListenable(Runnable task) {
    try {
      ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
      doExecute(this.concurrentExecutor, this.taskDecorator, future);
      return future;
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
    }
  }

  @Override
  public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
    try {
      ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
      doExecute(this.concurrentExecutor, this.taskDecorator, future);
      return future;
    }
    catch (RejectedExecutionException ex) {
      throw new TaskRejectedException(
              "Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
    }
  }

  /**
   * Actually execute the given {@code Runnable} (which may be a user-supplied task
   * or a wrapper around a user-supplied task) with the given executor.
   *
   * @param concurrentExecutor
   *         the underlying JDK concurrent executor to delegate to
   * @param taskDecorator
   *         the specified decorator to be applied, if any
   * @param runnable
   *         the runnable to execute
   *
   * @throws RejectedExecutionException
   *         if the given runnable cannot be accepted
   */
  protected void doExecute(
          Executor concurrentExecutor, @Nullable TaskDecorator taskDecorator, Runnable runnable)
          throws RejectedExecutionException {

    concurrentExecutor.execute(taskDecorator != null ? taskDecorator.decorate(runnable) : runnable);
  }

}
