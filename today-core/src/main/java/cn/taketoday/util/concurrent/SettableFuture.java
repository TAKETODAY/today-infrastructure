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

import cn.taketoday.lang.Nullable;

/**
 * Special {@link Future} which is writable.
 * <p>
 *
 * A {@link Future} whose result can be set by a {@link #setSuccess(Object)},
 * {@link #setFailure(Throwable)} call. It can also, like any other {@code Future},
 * be {@linkplain #cancel cancelled}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 15:57
 */
public interface SettableFuture<V> extends Future<V> {

  /**
   * Marks this future as a success and notifies all
   * listeners.
   *
   * If it is success or failed already it will throw an {@link IllegalStateException}.
   */
  SettableFuture<V> setSuccess(@Nullable V result);

  /**
   * Marks this future as a success and notifies all
   * listeners.
   *
   * @return {@code true} if and only if successfully marked this future as
   * a success. Otherwise {@code false} because this future is
   * already marked as either a success or a failure.
   */
  boolean trySuccess(@Nullable V result);

  /**
   * Marks this future as a failure and notifies all
   * listeners.
   *
   * If it is success or failed already it will throw an {@link IllegalStateException}.
   */
  SettableFuture<V> setFailure(Throwable cause);

  /**
   * Marks this future as a failure and notifies all
   * listeners.
   *
   * @return {@code true} if and only if successfully marked this future as
   * a failure. Otherwise {@code false} because this future is
   * already marked as either a success or a failure.
   */
  boolean tryFailure(Throwable cause);

  @Override
  default SettableFuture<V> onCompleted(SuccessCallback<V> successCallback, @Nullable FailureCallback failureCallback) {
    return onCompleted(FutureListener.forAdaption(successCallback, failureCallback));
  }

  @Override
  default SettableFuture<V> onSuccess(SuccessCallback<V> successCallback) {
    return onCompleted(successCallback, null);
  }

  @Override
  default SettableFuture<V> onFailure(FailureCallback failureCallback) {
    onCompleted(FutureListener.forFailure(failureCallback));
    return this;
  }

  @Override
  SettableFuture<V> onCompleted(FutureListener<? extends Future<V>> listener);

  @Override
  <C> SettableFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context);

  @Override
  SettableFuture<V> await() throws InterruptedException;

  @Override
  SettableFuture<V> awaitUninterruptibly();

  @Override
  SettableFuture<V> sync() throws InterruptedException;

  @Override
  SettableFuture<V> syncUninterruptibly();

  @Override
  default SettableFuture<V> cascadeTo(final SettableFuture<V> settable) {
    Futures.cascade(this, settable);
    return this;
  }

}
