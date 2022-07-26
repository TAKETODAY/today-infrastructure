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

package cn.taketoday.retry.listener;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.TerminatedRetryException;
import cn.taketoday.retry.interceptor.MethodInvocationRetryCallback;

/**
 * <p>
 * Empty method implementation of {@link RetryListener} with focus on the AOP reflective
 * method invocations providing convenience retry listener type-safe (with a
 * `MethodInvocationRetryCallback` callback parameter) specific methods.
 * </p>
 * NOTE that this listener performs an action only when dealing with callbacks that are
 * instances of {@link MethodInvocationRetryCallback}.
 *
 * @author Marius Grama
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MethodInvocationRetryListenerSupport implements RetryListener {

  @Override
  public <T, E extends Throwable> void close(
          RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    if (callback instanceof MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback) {
      doClose(context, methodInvocationRetryCallback, throwable);
    }
  }

  @Override
  public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
    if (callback instanceof MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback) {
      doOnSuccess(context, methodInvocationRetryCallback, result);
    }
  }

  @Override
  public <T, E extends Throwable> void onError(
          RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    if (callback instanceof MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback) {
      doOnError(context, methodInvocationRetryCallback, throwable);
    }
  }

  @Override
  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    if (callback instanceof MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback) {
      return doOpen(context, methodInvocationRetryCallback);
    }
    // in case that the callback is not for a reflective method invocation
    // just go forward with the execution
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
  protected <T, E extends Throwable> void doClose(RetryContext context, MethodInvocationRetryCallback<T, E> callback,
          Throwable throwable) {
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
  protected <T, E extends Throwable> void doOnSuccess(RetryContext context,
          MethodInvocationRetryCallback<T, E> callback, T result) {
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
  protected <T, E extends Throwable> void doOnError(RetryContext context,
          MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
  }

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
  protected <T, E extends Throwable> boolean doOpen(RetryContext context,
          MethodInvocationRetryCallback<T, E> callback) {
    return true;
  }

}
