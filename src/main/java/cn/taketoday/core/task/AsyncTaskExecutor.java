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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Extended interface for asynchronous {@link TaskExecutor} implementations,
 * offering an overloaded {@link #execute(Runnable, long)} variant with a start
 * timeout parameter as well support for {@link Callable}.
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
 * @see SimpleAsyncTaskExecutor
 * @see Callable
 * @see java.util.concurrent.Executors
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
  void execute(Runnable task, long startTimeout);

  /**
   * Submit a Runnable task for execution, receiving a Future representing that task.
   * The Future will return a {@code null} result upon completion.
   *
   * @param task the {@code Runnable} to execute (never {@code null})
   * @return a Future representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  Future<?> submit(Runnable task);

  /**
   * Submit a Callable task for execution, receiving a Future representing that task.
   * The Future will return the Callable's result upon completion.
   *
   * @param task the {@code Callable} to execute (never {@code null})
   * @return a Future representing pending completion of the task
   * @throws TaskRejectedException if the given task was not accepted
   */
  <T> Future<T> submit(Callable<T> task);

}
