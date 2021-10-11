/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.util.ArrayDeque;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Helper class for {@link ListenableFuture} implementations that maintains a
 * of success and failure callbacks and helps to notify them.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ExecutionList}.
 *
 * @param <T>
 *         the callback result type
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ListenableFutureCallbackRegistry<T> {

  private final ArrayDeque<SuccessCallback<? super T>> successCallbacks = new ArrayDeque<>(1);

  private final ArrayDeque<FailureCallback> failureCallbacks = new ArrayDeque<>(1);

  private State state = State.NEW;

  @Nullable
  private Object result;

  private final Object mutex = new Object();

  /**
   * Add the given callback to this registry.
   *
   * @param callback
   *         the callback to add
   */
  public void addCallback(ListenableFutureCallback<? super T> callback) {
    Assert.notNull(callback, "'callback' must not be null");
    synchronized(this.mutex) {
      switch (this.state) {
        case NEW:
          this.successCallbacks.add(callback);
          this.failureCallbacks.add(callback);
          break;
        case SUCCESS:
          notifySuccess(callback);
          break;
        case FAILURE:
          notifyFailure(callback);
          break;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void notifySuccess(SuccessCallback<? super T> callback) {
    try {
      callback.onSuccess((T) this.result);
    }
    catch (Throwable ex) {
      // Ignore
    }
  }

  private void notifyFailure(FailureCallback callback) {
    Assert.state(this.result instanceof Throwable, "No Throwable result for failure state");
    try {
      callback.onFailure((Throwable) this.result);
    }
    catch (Throwable ex) {
      // Ignore
    }
  }

  /**
   * Add the given success callback to this registry.
   *
   * @param callback
   *         the success callback to add
   *
   * @since 4.1
   */
  public void addSuccessCallback(SuccessCallback<? super T> callback) {
    Assert.notNull(callback, "'callback' must not be null");
    synchronized(this.mutex) {
      switch (this.state) {
        case NEW:
          this.successCallbacks.add(callback);
          break;
        case SUCCESS:
          notifySuccess(callback);
          break;
      }
    }
  }

  /**
   * Add the given failure callback to this registry.
   *
   * @param callback
   *         the failure callback to add
   *
   * @since 4.1
   */
  public void addFailureCallback(FailureCallback callback) {
    Assert.notNull(callback, "'callback' must not be null");
    synchronized(this.mutex) {
      switch (this.state) {
        case NEW:
          this.failureCallbacks.add(callback);
          break;
        case FAILURE:
          notifyFailure(callback);
          break;
      }
    }
  }

  /**
   * Trigger a {@link ListenableFutureCallback#onSuccess(Object)} call on all
   * added callbacks with the given result.
   *
   * @param result
   *         the result to trigger the callbacks with
   */
  public void success(@Nullable T result) {
    synchronized(this.mutex) {
      this.state = State.SUCCESS;
      this.result = result;
      SuccessCallback<? super T> callback;
      while ((callback = this.successCallbacks.poll()) != null) {
        notifySuccess(callback);
      }
    }
  }

  /**
   * Trigger a {@link ListenableFutureCallback#onFailure(Throwable)} call on all
   * added callbacks with the given {@code Throwable}.
   *
   * @param ex
   *         the exception to trigger the callbacks with
   */
  public void failure(Throwable ex) {
    synchronized(this.mutex) {
      this.state = State.FAILURE;
      this.result = ex;
      FailureCallback callback;
      while ((callback = this.failureCallbacks.poll()) != null) {
        notifyFailure(callback);
      }
    }
  }

  private enum State {
    NEW, SUCCESS, FAILURE
  }

}
