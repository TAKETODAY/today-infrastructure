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

package infra.core.task.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import infra.core.task.AsyncTaskExecutor;
import infra.core.task.TaskDecorator;
import infra.core.task.TaskExecutor;
import infra.core.task.TaskRejectedException;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.concurrent.Future;

/**
 * Adapter that takes a JDK {@code java.util.concurrent.Executor} and
 * exposes a {@link TaskExecutor} for it.
 * Also detects an extended {@code java.util.concurrent.ExecutorService}, adapting
 * the {@link AsyncTaskExecutor} interface accordingly.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Executor
 * @see ExecutorService
 * @see java.util.concurrent.Executors
 * @since 4.0
 */
public class TaskExecutorAdapter implements AsyncTaskExecutor {

  private final Executor concurrentExecutor;

  @Nullable
  private TaskDecorator taskDecorator;

  /**
   * Create a new TaskExecutorAdapter,
   * using the given JDK concurrent executor.
   *
   * @param concurrentExecutor the JDK concurrent executor to delegate to
   */
  public TaskExecutorAdapter(Executor concurrentExecutor) {
    Assert.notNull(concurrentExecutor, "Executor is required");
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
      throw new TaskRejectedException(this.concurrentExecutor, task, ex);
    }
  }

  @Override
  public Future<Void> submit(Runnable task) {
    var future = Future.<Void>forFutureTask(task, this);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    var future = Future.forFutureTask(task, this);
    execute(future, TIMEOUT_INDEFINITE);
    return future;
  }

  /**
   * Actually execute the given {@code Runnable} (which may be a user-supplied task
   * or a wrapper around a user-supplied task) with the given executor.
   *
   * @param concurrentExecutor the underlying JDK concurrent executor to delegate to
   * @param taskDecorator the specified decorator to be applied, if any
   * @param runnable the runnable to execute
   * @throws RejectedExecutionException if the given runnable cannot be accepted
   */
  protected void doExecute(Executor concurrentExecutor,
          @Nullable TaskDecorator taskDecorator, Runnable runnable) throws RejectedExecutionException {

    concurrentExecutor.execute(taskDecorator != null ? taskDecorator.decorate(runnable) : runnable);
  }

}
