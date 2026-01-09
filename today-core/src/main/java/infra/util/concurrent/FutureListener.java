/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  static <V extends @Nullable Object, F extends Future<V>, C extends @Nullable Object> FutureListener<F> forAdaption(FutureContextListener<F, C> listener, @Nullable C context) {
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
