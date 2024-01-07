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

import cn.taketoday.util.concurrent.ListenableFuture;

/**
 * Extension of the {@link AsyncTaskExecutor} interface, adding the capability to submit
 * tasks for {@link ListenableFuture ListenableFutures}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ListenableFuture
 * @since 4.0
 */
public interface AsyncListenableTaskExecutor extends AsyncTaskExecutor {

  /**
   * Submit a {@code Runnable} task for execution, receiving a {@code ListenableFuture}
   * representing that task. The Future will return a {@code null} result upon completion.
   * <p>
   * in favor of {@link AsyncTaskExecutor#submitCompletable(Runnable)}
   *
   * @param task the {@code Runnable} to execute (never {@code null})
   * @return a {@code ListenableFuture} representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  ListenableFuture<?> submitListenable(Runnable task);

  /**
   * Submit a {@code Callable} task for execution, receiving a {@code ListenableFuture}
   * representing that task. The Future will return the Callable's result upon
   * completion.
   * <p>
   * in favor of {@link AsyncTaskExecutor#submitCompletable(Callable)}
   *
   * @param task the {@code Callable} to execute (never {@code null})
   * @return a {@code ListenableFuture} representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  <T> ListenableFuture<T> submitListenable(Callable<T> task);

}
