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

import java.util.EventListener;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Listens to the result of a {@link Future}.
 * The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#onCompleted(FutureListener)}.
 *
 * @param <F> the future type
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface FutureListener<F extends Future<?>> extends EventListener {

  /**
   * Invoked when the operation associated with
   * the {@link Future} has been completed.
   *
   * @param completed the source {@link Future} which called this callback
   */
  void operationComplete(F completed) throws Throwable;

  // Static Factory Methods

  static <V, F extends Future<V>, C> FutureListener<F> forAdaption(FutureContextListener<F, C> listener, @Nullable C context) {
    Assert.notNull(listener, "listener is required");
    return future -> listener.operationComplete(future, context);
  }

  /**
   * Java 8 lambda-friendly alternative with success and failure callbacks.
   *
   * @param onSuccess success callback
   * @param onFailure failure callback
   * @param <F> ListenableFuture sub-type
   */
  static <V, F extends Future<V>> FutureListener<F> forAdaption(SuccessCallback<V> onSuccess, @Nullable FailureCallback onFailure) {
    Assert.notNull(onSuccess, "successCallback is required");
    return future -> {
      if (future.isSuccess()) {
        onSuccess.onSuccess(future.getNow());
      }
      else if (onFailure != null) {
        FailureCallback.onFailure(future, onFailure);
      }
    };
  }

  /**
   * Java 8 lambda-friendly alternative with failure callbacks.
   *
   * @param failureCallback the failure callback
   */
  static <V, F extends Future<V>> FutureListener<F> forFailure(FailureCallback failureCallback) {
    Assert.notNull(failureCallback, "failureCallback is required");
    return future -> FailureCallback.onFailure(future, failureCallback);
  }

}
