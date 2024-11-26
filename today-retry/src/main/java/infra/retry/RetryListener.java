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

package infra.retry;

/**
 * Interface for listener that can be used to add behaviour to a retry. Implementations of
 * {@link RetryOperations} can chose to issue callbacks to an interceptor during the retry
 * lifecycle.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
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
  default <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    return true;
  }

  /**
   * Called after the final attempt (successful or not). Allow the listener to clean up
   * any resource it is holding before control returns to the retry caller.
   *
   * @param context the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @param throwable the last exception that was thrown by the callback.
   * @param <E> the exception type
   * @param <T> the return value
   */
  default <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

  }

  /**
   * Called after a successful attempt; allow the listener to throw a new exception to
   * cause a retry (according to the retry policy), based on the result returned by the
   * {@link RetryCallback#doWithRetry(RetryContext)}
   *
   * @param <T> the return type.
   * @param context the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @param result the result returned by the callback method.
   */
  default <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {

  }

  /**
   * Called after every unsuccessful attempt at a retry.
   *
   * @param context the current {@link RetryContext}.
   * @param callback the current {@link RetryCallback}.
   * @param throwable the last exception that was thrown by the callback.
   * @param <T> the return value
   * @param <E> the exception to throw
   */
  default <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

  }

}
