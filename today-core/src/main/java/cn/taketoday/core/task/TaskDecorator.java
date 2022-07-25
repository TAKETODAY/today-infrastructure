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

/**
 * A callback interface for a decorator to be applied to any {@link Runnable}
 * about to be executed.
 *
 * <p>Note that such a decorator is not necessarily being applied to the
 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
 * execution callback (which may be a wrapper around the user-supplied task).
 *
 * <p>The primary use case is to set some execution context around the task's
 * invocation, or to provide some monitoring/statistics for task execution.
 *
 * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
 * may be limited. Specifically in case of a {@code Future}-based operation,
 * the exposed {@code Runnable} will be a wrapper which does not propagate
 * any exceptions from its {@code run} method.
 *
 * @author Juergen Hoeller
 * @see TaskExecutor#execute(Runnable)
 * @see SimpleAsyncTaskExecutor#setTaskDecorator
 * @see cn.taketoday.core.task.support.TaskExecutorAdapter#setTaskDecorator
 * @since 4.0
 */
@FunctionalInterface
public interface TaskDecorator {

  /**
   * Decorate the given {@code Runnable}, returning a potentially wrapped
   * {@code Runnable} for actual execution, internally delegating to the
   * original {@link Runnable#run()} implementation.
   *
   * @param runnable the original {@code Runnable}
   * @return the decorated {@code Runnable}
   */
  Runnable decorate(Runnable runnable);

}
