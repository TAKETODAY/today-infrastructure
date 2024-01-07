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

import java.util.ArrayDeque;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Helper class for {@link ListenableFuture} implementations that maintains a
 * of success and failure callbacks and helps to notify them.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ExecutionList}.
 *
 * @param <T> the callback result type
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ListenableFutureListenerRegistry<T> {
  private static final Logger logger = LoggerFactory.getLogger(ListenableFutureListenerRegistry.class);

  private final ArrayDeque<FutureListener<? super T>> futureListeners = new ArrayDeque<>(1);

  private State state = State.NEW;

  @Nullable
  private Object result;

  private final Object mutex = new Object();

  /**
   * Add the given callback to this registry.
   *
   * @param listener the callback to add
   */
  @SuppressWarnings("unchecked")
  public void addListener(FutureListener<? super T> listener) {
    Assert.notNull(listener, "'listener' is required");
    synchronized(this.mutex) {
      switch (this.state) {
        case NEW -> futureListeners.add(listener);
        case SUCCESS -> notifySuccess(listener, (T) result);
        case FAILURE -> {
          Assert.state(result instanceof Throwable, "No Throwable result for failure state");
          notifyFailure(listener, (Throwable) result);
        }
      }
    }
  }

  private void notifySuccess(FutureListener<? super T> listener, T result) {
    try {
      listener.onSuccess(result);
    }
    catch (Throwable ex) {
      logger.warn("FutureListener: {} notifySuccess failed", listener, ex);
    }
  }

  private void notifyFailure(FutureListener<? super T> listener, Throwable result) {
    try {
      listener.onFailure(result);
    }
    catch (Throwable ex) {
      logger.warn("FutureListener: {} notifyFailure failed", listener, ex);
    }
  }

  /**
   * Trigger a {@link FutureListener#onSuccess(Object)} call on all
   * added callbacks with the given result.
   *
   * @param result the result to trigger the callbacks with
   */
  public void success(@Nullable T result) {
    synchronized(this.mutex) {
      this.state = State.SUCCESS;
      this.result = result;
      FutureListener<? super T> listener;
      while ((listener = futureListeners.poll()) != null) {
        notifySuccess(listener, result);
      }
    }
  }

  /**
   * Trigger a {@link FutureListener#onFailure(Throwable)} call on all
   * added callbacks with the given {@code Throwable}.
   *
   * @param ex the exception to trigger the callbacks with
   */
  public void failure(Throwable ex) {
    synchronized(this.mutex) {
      this.state = State.FAILURE;
      this.result = ex;
      FutureListener<? super T> listener;
      while ((listener = futureListeners.poll()) != null) {
        notifyFailure(listener, ex);
      }
    }
  }

  private enum State {
    NEW, SUCCESS, FAILURE
  }

}
