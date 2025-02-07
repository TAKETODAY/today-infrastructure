/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.socket;

import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @param <V> value type
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class PromiseAdapter<V> implements GenericFutureListener<io.netty.util.concurrent.Future<V>> {

  private final Promise<V> promise;

  PromiseAdapter(Promise<V> promise) {
    this.promise = promise;
  }

  @Override
  public void operationComplete(io.netty.util.concurrent.Future<V> future) {
    Throwable cause = future.cause();
    if (cause != null) {
      promise.tryFailure(cause);
    }
    else {
      promise.trySuccess(future.getNow());
    }
  }

  public static <T> Future<T> adapt(io.netty.util.concurrent.Future<T> future) {
    return adapt(future, Future.forPromise());
  }

  public static <T> Future<T> adapt(io.netty.util.concurrent.Future<T> future, Promise<T> settable) {
    future.addListener(new PromiseAdapter<>(settable));
    return settable;
  }
}
