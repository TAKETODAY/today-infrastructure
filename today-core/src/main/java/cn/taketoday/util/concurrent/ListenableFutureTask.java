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

package cn.taketoday.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import cn.taketoday.lang.Nullable;

/**
 * RunnableFuture
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FutureTask
 * @since 4.0
 */
public class ListenableFutureTask<T> extends SettableFutureTask<T> implements ListenableFuture<T> {

  /**
   * Create a new {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Callable}.
   *
   * @param callable the callable task
   */
  public ListenableFutureTask(Callable<T> callable) {
    super(null, callable);
  }

  /**
   * Create a {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Runnable}, and arrange that {@link #get()}
   * will return the given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion
   */
  public ListenableFutureTask(Runnable runnable, @Nullable T result) {
    super(null, runnable, result);
  }

}
