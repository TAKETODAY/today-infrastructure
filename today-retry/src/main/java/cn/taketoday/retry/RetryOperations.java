/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry;

import cn.taketoday.retry.support.DefaultRetryState;

/**
 * Defines the basic set of operations implemented by {@link RetryOperations} to execute
 * operations with configurable retry behaviour.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @since 4.0
 */
public interface RetryOperations {

  /**
   * Execute the supplied {@link RetryCallback} with the configured retry semantics. See
   * implementations for configuration details.
   *
   * @param <T> the return value
   * @param retryCallback the {@link RetryCallback}
   * @param <E> the exception to throw
   * @return the value returned by the {@link RetryCallback} upon successful invocation.
   * @throws E any {@link Exception} raised by the {@link RetryCallback} upon
   * unsuccessful retry.
   * @throws E the exception thrown
   */
  <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E;

  /**
   * Execute the supplied {@link RetryCallback} with a fallback on exhausted retry to
   * the {@link RecoveryCallback}. See implementations for configuration details.
   *
   * @param recoveryCallback the {@link RecoveryCallback}
   * @param retryCallback the {@link RetryCallback} {@link RecoveryCallback} upon
   * @param <T> the type to return
   * @param <E> the type of the exception
   * @return the value returned by the {@link RetryCallback} upon successful invocation,
   * and that returned by the {@link RecoveryCallback} otherwise.
   * @throws E any {@link Exception} raised by the unsuccessful retry.
   */
  <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback)
          throws E;

  /**
   * A simple stateful retry. Execute the supplied {@link RetryCallback} with a target
   * object for the attempt identified by the {@link DefaultRetryState}. Exceptions
   * thrown by the callback are always propagated immediately so the state is required
   * to be able to identify the previous attempt, if there is one - hence the state is
   * required. Normal patterns would see this method being used inside a transaction,
   * where the callback might invalidate the transaction if it fails.
   *
   * See implementations for configuration details.
   *
   * @param retryCallback the {@link RetryCallback}
   * @param retryState the {@link RetryState}
   * @param <T> the type of the return value
   * @param <E> the type of the exception to return
   * @return the value returned by the {@link RetryCallback} upon successful invocation,
   * and that returned by the {@link RecoveryCallback} otherwise.
   * @throws E any {@link Exception} raised by the {@link RecoveryCallback}.
   * @throws ExhaustedRetryException if the last attempt for this state has already been
   * reached
   */
  <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RetryState retryState)
          throws E, ExhaustedRetryException;

  /**
   * A stateful retry with a recovery path. Execute the supplied {@link RetryCallback}
   * with a fallback on exhausted retry to the {@link RecoveryCallback} and a target
   * object for the retry attempt identified by the {@link DefaultRetryState}.
   *
   * @param recoveryCallback the {@link RecoveryCallback}
   * @param retryState the {@link RetryState}
   * @param retryCallback the {@link RetryCallback}
   * @param <T> the return value type
   * @param <E> the exception type
   * @return the value returned by the {@link RetryCallback} upon successful invocation,
   * and that returned by the {@link RecoveryCallback} otherwise.
   * @throws E any {@link Exception} raised by the {@link RecoveryCallback} upon
   * unsuccessful retry.
   * @see #execute(RetryCallback, RetryState)
   */
  <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback,
          RetryState retryState) throws E;

}
