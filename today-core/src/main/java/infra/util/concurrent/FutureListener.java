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

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.EventListener;

import infra.lang.Assert;

/**
 * Listens to the result of a {@link Future}.
 * <p>
 * The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#onCompleted(FutureListener)}.
 *
 * @param <F> the future type
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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

  @SuppressWarnings("NullAway")
  static <V, F extends Future<V>, C> FutureListener<F> forAdaption(FutureContextListener<F, C> listener, @Nullable C context) {
    Assert.notNull(listener, "listener is required");
    return future -> listener.operationComplete(future, context);
  }

  /**
   * Java 8 lambda-friendly alternative with success and failure callbacks.
   *
   * <p> Adapts {@link AbstractFuture#trySuccess(Object)},
   * {@link Promise#setFailure(Throwable)} and
   * {@link AbstractFuture#tryFailure(Throwable)} operations
   *
   * @param onSuccess success callback
   * @param onFailed failed callback
   * @param <F> Future subtype
   * @see AbstractFuture#trySuccess(Object)
   * @see Promise#setFailure(Throwable)
   * @see AbstractFuture#tryFailure(Throwable)
   */
  @SuppressWarnings("NullAway")
  static <V, F extends Future<V>> FutureListener<F> forAdaption(SuccessCallback<V> onSuccess, @Nullable FailureCallback onFailed) {
    Assert.notNull(onSuccess, "successCallback is required");
    return future -> {
      if (future.isSuccess()) {
        onSuccess.onSuccess(future.getNow());
      }
      else if (onFailed != null) {
        onFailure(future, onFailed);
      }
    };
  }

  /**
   * Creates non-cancelled {@link Future#isFailed() failed} FutureListener
   *
   * <p>Java 8 lambda-friendly alternative with failure callbacks.
   *
   * @param failureCallback the failure callback
   * @see Promise#setFailure(Throwable)
   * @see AbstractFuture#tryFailure(Throwable)
   * @see Future#isFailure()
   */
  static <V, F extends Future<V>> FutureListener<F> forFailure(FailureCallback failureCallback) {
    Assert.notNull(failureCallback, "failureCallback is required");
    return future -> onFailure(future, failureCallback);
  }

  /**
   * Creates {@link Future#isFailed() failed} FutureListener
   *
   * <p>Future maybe cancelled
   *
   * @param failedCallback the failed callback
   * @see Future#isFailed()
   * @see Future#isCancelled()
   * @since 5.0
   */
  static <V, F extends Future<V>> FutureListener<F> forFailed(FailureCallback failedCallback) {
    Assert.notNull(failedCallback, "failedCallback is required");
    return future -> {
      Throwable cause = future.getCause();
      if (cause != null) {
        failedCallback.onFailure(cause);
      }
    };
  }

  /**
   * on failure callback
   * <p> non-cancelled {@link Future#isFailed() failed} Future
   *
   * @param future target future
   * @param failureCallback failure callback
   * @param <V> value type
   * @see Future#isFailure()
   * @since 5.0
   */
  private static <V> void onFailure(Future<V> future, FailureCallback failureCallback) throws Throwable {
    if (!future.isCancelled()) {
      Throwable cause = future.getCause();
      if (cause != null) {
        failureCallback.onFailure(cause);
      }
    }
  }

}
