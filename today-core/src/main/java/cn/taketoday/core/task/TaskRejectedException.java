/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link TaskExecutor} rejects to accept
 * a given task for execution.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TaskExecutor#execute(Runnable)
 * @see TaskTimeoutException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TaskRejectedException extends RejectedExecutionException {

  /**
   * Create a new {@code TaskRejectedException}
   * with the specified detail message and no root cause.
   *
   * @param msg the detail message
   */
  public TaskRejectedException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code TaskRejectedException}
   * with the specified detail message and the given root cause.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using an underlying
   * API such as the {@code java.util.concurrent} package)
   * @see java.util.concurrent.RejectedExecutionException
   */
  public TaskRejectedException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Create a new {@code TaskRejectedException}
   * with a default message for the given executor and task.
   *
   * @param executor the {@code Executor} that rejected the task
   * @param task the task object that got rejected
   * @param cause the original {@link RejectedExecutionException}
   * @see ExecutorService#isShutdown()
   * @see java.util.concurrent.RejectedExecutionException
   */
  public TaskRejectedException(Executor executor, Object task, RejectedExecutionException cause) {
    super(executorDescription(executor) + " did not accept task: " + task, cause);
  }

  private static String executorDescription(Executor executor) {
    if (executor instanceof ExecutorService executorService) {
      return "ExecutorService in " + (executorService.isShutdown() ? "shutdown" : "active") + " state";
    }
    return executor.toString();
  }

}
