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
 * @since 4.0
 */
public class MethodInvocationRetryListenerSupport implements RetryListener {

  public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
          Throwable throwable) {
    if (callback instanceof MethodInvocationRetryCallback) {
      MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>) callback;
      doClose(context, methodInvocationRetryCallback, throwable);
    }
  }

  public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
          Throwable throwable) {
    if (callback instanceof MethodInvocationRetryCallback) {
      MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>) callback;
      doOnError(context, methodInvocationRetryCallback, throwable);
    }
  }

  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    if (callback instanceof MethodInvocationRetryCallback) {
      MethodInvocationRetryCallback<T, E> methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>) callback;
      return doOpen(context, methodInvocationRetryCallback);
    }
    // in case that the callback is not for a reflective method invocation
    // just go forward with the execution
    return true;
  }

  protected <T, E extends Throwable> void doClose(RetryContext context, MethodInvocationRetryCallback<T, E> callback,
          Throwable throwable) {
  }

  protected <T, E extends Throwable> void doOnError(RetryContext context,
          MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
  }

  protected <T, E extends Throwable> boolean doOpen(RetryContext context,
          MethodInvocationRetryCallback<T, E> callback) {
    return true;
  }

}
