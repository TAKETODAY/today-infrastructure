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
 * A listener that is notified when a {@link Future} operation completes.
 * <p>
 * This functional interface allows for asynchronous handling of future completion events,
 * regardless of whether the operation succeeded, failed, or was cancelled.
 *
 * @param <F> the type of the future this listener is associated with
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface FutureListener<F extends Future<?>> extends EventListener {

  /**
   * Invoked when the operation associated with the {@link Future} has been completed.
   * <p>
   * This method is called regardless of whether the future completed successfully,
   * failed, or was cancelled. Implementations should check the state of the future
   * using methods such as {@link Future#isSuccess()}, {@link Future#isFailed()},
   * or {@link Future#isCancelled()} to determine the outcome.
   *
   * @param completed the source {@link Future} which triggered this callback
   * @throws Throwable if an error occurs during the execution of this listener
   */
  void operationComplete(F completed) throws Throwable;

  // Static Factory Methods

  /**
   * Adapts a {@link FutureContextListener} with a context object to a {@link FutureListener}.
   *
   * @param listener the context listener to adapt, must not be null
   * @param context the context object to pass to the listener, may be null
   * @param <V> the result type of the future
   * @param <F> the future type
   * @param <C> the context type
   * @return a new {@link FutureListener} that delegates to the provided context listener
   * @throws IllegalArgumentException if the listener is null
   */
  static <V extends @Nullable Object, F extends Future<V>, C extends @Nullable Object> FutureListener<F> forAdaption(FutureContextListener<F, C> listener, @Nullable C context) {
    Assert.notNull(listener, "listener is required");
    return future -> listener.operationComplete(future, context);
  }

  /**
   * Creates a {@link FutureListener} that handles success and failure outcomes using separate callbacks.
   * <p>
   * This method provides a Java 8 lambda-friendly alternative for handling future completion.
   * The success callback is invoked if the future completes successfully. The failure callback
   * is invoked only if the future fails and is not cancelled. If the future is cancelled,
   * neither callback is invoked.
   *
   * @param onSuccess the callback to invoke on success, must not be null
   * @param onFailure the callback to invoke on failure (if not cancelled), may be null
   * @param <V> the result type of the future
   * @param <F> the type of the future
   * @return a new {@link FutureListener} that delegates to the provided callbacks
   * @throws IllegalArgumentException if the onSuccess callback is null
   * @see AbstractFuture#trySuccess(Object)
   * @see Promise#setFailure(Throwable)
   * @see AbstractFuture#tryFailure(Throwable)
   */
  static <V extends @Nullable Object, F extends Future<V>> FutureListener<F> forAdaption(SuccessCallback<V> onSuccess, @Nullable FailureCallback onFailure) {
    Assert.notNull(onSuccess, "successCallback is required");
    return future -> {
      if (future.isSuccess()) {
        onSuccess.onSuccess(future.getNow());
      }
      else if (onFailure != null) {
        onFailure(future, onFailure);
      }
    };
  }

  /**
   * Creates a {@link FutureListener} that invokes the failure callback only when the future fails and is not cancelled.
   * <p>
   * This method provides a Java 8 lambda-friendly alternative for handling failures.
   * The callback will not be invoked if the future is cancelled, even if it is considered failed.
   *
   * @param failureCallback the callback to invoke on failure, must not be null
   * @param <V> the result type of the future
   * @param <F> the type of the future
   * @return a new {@link FutureListener} that handles non-cancelled failures
   * @throws IllegalArgumentException if the failureCallback is null
   * @see Promise#setFailure(Throwable)
   * @see AbstractFuture#tryFailure(Throwable)
   * @see Future#isFailure()
   */
  static <V, F extends Future<V>> FutureListener<F> forFailure(FailureCallback failureCallback) {
    Assert.notNull(failureCallback, "failureCallback is required");
    return future -> onFailure(future, failureCallback);
  }

  /**
   * Creates a {@link FutureListener} that invokes the failure callback when the
   * future has a cause (failed or cancelled with exception).
   * <p>
   * Unlike {@link #forFailure(FailureCallback)}, this listener does not distinguish between cancellation and failure.
   * It simply checks if the future has a cause and invokes the callback if present.
   *
   * @param failedCallback the callback to invoke when the future has a cause, must not be null
   * @param <V> the result type of the future
   * @param <F> the type of the future
   * @return a new {@link FutureListener} that handles any completion with a cause
   * @throws IllegalArgumentException if the failedCallback is null
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
   * Handles the failure of a {@link Future} by invoking the provided callback.
   * <p>
   * This method checks if the future is not cancelled and has a cause,
   * then invokes the {@link FailureCallback#onFailure(Throwable)} with the cause.
   *
   * @param future the target future to check for failure
   * @param failureCallback the callback to invoke on failure, must not be null
   * @param <V> the result type of the future
   * @throws Throwable if the failure callback throws an exception
   * @see Future#isCancelled()
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
