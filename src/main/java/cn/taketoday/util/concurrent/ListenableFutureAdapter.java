/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util.concurrent;

import java.util.concurrent.ExecutionException;

import cn.taketoday.lang.Nullable;

/**
 * Abstract class that adapts a {@link ListenableFuture} parameterized over S into a
 * {@code ListenableFuture} parameterized over T. All methods are delegated to the
 * adaptee, where {@link #get()}, {@link #get(long, java.util.concurrent.TimeUnit)},
 * and {@link ListenableFutureCallback#onSuccess(Object)} call {@link #adapt(Object)}
 * on the adaptee's result.
 *
 * @param <T> the type of this {@code Future}
 * @param <S> the type of the adaptee's {@code Future}
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class ListenableFutureAdapter<T, S> extends FutureAdapter<T, S> implements ListenableFuture<T> {

  /**
   * Construct a new {@code ListenableFutureAdapter} with the given adaptee.
   *
   * @param adaptee the future to adapt to
   */
  protected ListenableFutureAdapter(ListenableFuture<S> adaptee) {
    super(adaptee);
  }

  @Override
  public void addCallback(final ListenableFutureCallback<? super T> callback) {
    addCallback(callback, callback);
  }

  @Override
  public void addCallback(final SuccessCallback<? super T> successCallback, final FailureCallback failureCallback) {
    ListenableFuture<S> listenableAdaptee = (ListenableFuture<S>) getAdaptee();
    listenableAdaptee.addCallback(new ListenableFutureCallback<S>() {
      @Override
      public void onSuccess(@Nullable S result) {
        T adapted = null;
        if (result != null) {
          try {
            adapted = adaptInternal(result);
          }
          catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            onFailure(cause != null ? cause : ex);
            return;
          }
          catch (Throwable ex) {
            onFailure(ex);
            return;
          }
        }
        successCallback.onSuccess(adapted);
      }

      @Override
      public void onFailure(Throwable ex) {
        failureCallback.onFailure(ex);
      }
    });
  }

}
