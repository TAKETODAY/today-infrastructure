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

/**
 * Interface for listener that can be used to add behaviour to a retry. Implementations of
 * {@link RetryOperations} can chose to issue callbacks to an interceptor during the retry
 * lifecycle.
 *
 * @author Dave Syer
 */
public interface RetryListener {

  /**
   * Called before the first attempt in a retry. For instance, implementers can set up
   * state that is needed by the policies in the {@link RetryOperations}. The whole
   * retry can be vetoed by returning false from this method, in which case a
   * {@link TerminatedRetryException} will be thrown.
   *
   * @param <T> the type of object returned by the callback
   * @param <E> the type of exception it declares may be thrown
   * @param context the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @return true if the retry should proceed.
   */
  <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback);

  /**
   * Called after the final attempt (successful or not). Allow the interceptor to clean
   * up any resource it is holding before control returns to the retry caller.
   *
   * @param context the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @param throwable the last exception that was thrown by the callback.
   * @param <E> the exception type
   * @param <T> the return value
   */
  <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable);

  /**
   * Called after every unsuccessful attempt at a retry.
   *
   * @param context the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @param throwable the last exception that was thrown by the callback.
   * @param <T> the return value
   * @param <E> the exception to throw
   */
  <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable);

}
