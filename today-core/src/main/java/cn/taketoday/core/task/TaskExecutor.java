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

import java.util.concurrent.Executor;

/**
 * Simple task executor interface that abstracts the execution
 * of a {@link Runnable}.
 *
 * <p>Implementations can use all sorts of different execution strategies,
 * such as: synchronous, asynchronous, using a thread pool, and more.
 *
 * <p>Equivalent to Java's {@link java.util.concurrent.Executor} interface,
 * so that clients may declare a dependency on an {@code Executor} and receive
 * any {@code TaskExecutor} implementation. This interface remains separate from
 * the standard {@code Executor} interface primarily for backwards compatibility
 * with older APIs that depend on the {@code TaskExecutor} interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * @param task the {@code Runnable} to execute (never {@code null})
   * @throws TaskRejectedException if the given task was not accepted
   */
  @Override
  void execute(Runnable task);

}
