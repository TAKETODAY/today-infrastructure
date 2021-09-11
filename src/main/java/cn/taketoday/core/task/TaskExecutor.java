/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.util.concurrent.Executor;

/**
 * Simple task executor interface that abstracts the execution
 * of a {@link Runnable}.
 *
 * <p>Implementations can use all sorts of different execution strategies,
 * such as: synchronous, asynchronous, using a thread pool, and more.
 *
 * @author Juergen Hoeller
 * @see Executor
 * @since 4.0
 */
@FunctionalInterface
public interface TaskExecutor extends Executor {

  /**
   * Execute the given {@code task}.
   * <p>The call might return immediately if the implementation uses
   * an asynchronous execution strategy, or might block in the case
   * of synchronous execution.
   *
   * @param task
   *         the {@code Runnable} to execute (never {@code null})
   *
   * @throws TaskRejectedException
   *         if the given task was not accepted
   */
  @Override
  void execute(Runnable task);

}
