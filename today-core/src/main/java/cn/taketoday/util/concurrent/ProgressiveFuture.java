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
 * A {@link SettableFuture} which is used to indicate the progress of an operation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProgressiveFutureListener
 * @since 4.0 2024/2/26 20:48
 */
public interface ProgressiveFuture<V> extends SettableFuture<V> {

  /**
   * Sets the current progress of the operation and notifies the listeners that implement
   * {@link ProgressiveFutureListener}.
   */
  ProgressiveFuture<V> setProgress(long progress, long total);

  /**
   * Tries to set the current progress of the operation and notifies the listeners that implement
   * {@link ProgressiveFutureListener}.  If the operation is already complete or the progress is out of range,
   * this method does nothing but returning {@code false}.
   */
  boolean tryProgress(long progress, long total);

  @Override
  default ProgressiveFuture<V> addListener(SuccessCallback<V> successCallback, @Nullable FailureCallback failureCallback) {
    return addListener(FutureListener.forAdaption(successCallback, failureCallback));
  }

  @Override
  default ProgressiveFuture<V> onSuccess(SuccessCallback<V> successCallback) {
    return addListener(successCallback, null);
  }

  @Override
  default ProgressiveFuture<V> onFailure(FailureCallback failureCallback) {
    addListener(FutureListener.forFailure(failureCallback));
    return this;
  }

  @Override
  <C> ProgressiveFuture<V> addListener(FutureContextListener<C, ? extends Future<V>> listener, @Nullable C context);

  @Override
  ProgressiveFuture<V> addListener(FutureListener<? extends Future<V>> listener);

  @Override
  ProgressiveFuture<V> sync() throws InterruptedException;

  @Override
  ProgressiveFuture<V> syncUninterruptibly();

  @Override
  ProgressiveFuture<V> await() throws InterruptedException;

  @Override
  ProgressiveFuture<V> awaitUninterruptibly();

}