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
 * Callback mechanism for the outcome, success or failure, from a
 * {@link ListenableFuture}.
 *
 * @param <T> the result type
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface FutureListener<T> extends SuccessCallback<T>, FailureCallback {

  /**
   * Called when the {@link ListenableFuture} completes with success.
   * <p>Note that Exceptions raised by this method are ignored.
   *
   * @param result the result
   */
  void onSuccess(@Nullable T result);

  /**
   * Called when the {@link ListenableFuture} completes with failure.
   * <p>Note that Exceptions raised by this method are ignored.
   *
   * @param ex the failure
   */
  void onFailure(Throwable ex);

  static <T> FutureListener<T> forListenable(SuccessCallback<T> successCallback, FailureCallback failureCallback) {
    return new FutureListener<>() {
      @Override
      public void onSuccess(@Nullable T result) {
        successCallback.onSuccess(result);
      }

      @Override
      public void onFailure(Throwable ex) {
        failureCallback.onFailure(ex);
      }
    };
  }

}