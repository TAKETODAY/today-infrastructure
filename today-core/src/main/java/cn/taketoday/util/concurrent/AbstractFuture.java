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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;

/**
 * Abstract {@link Future} implementation which does not allow for cancellation.
 *
 * @param <V>
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractFuture<V> implements Future<V> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractFuture.class);

  private static final Logger rejectedExecutionLogger =
          LoggerFactory.getLogger(AbstractFuture.class.getName() + ".rejectedExecution");

  /**
   * One or more listeners.
   */
  @Nullable
  private Object listeners;

  protected final Executor executor;

  protected AbstractFuture(@Nullable Executor executor) {
    this.executor = executor == null ? defaultExecutor : executor;
  }

  @Override
  public Executor executor() {
    return executor;
  }

  @Override
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    try {
      return await(timeout, unit);
    }
    catch (InterruptedException e) {
      // Should not be raised at all.
      throw new InternalError();
    }
  }

  @Override
  public boolean awaitUninterruptibly(long timeoutMillis) {
    try {
      return await(timeoutMillis);
    }
    catch (InterruptedException e) {
      // Should not be raised at all.
      throw new InternalError();
    }
  }

  @Override
  public V obtain() {
    V now = getNow();
    Assert.state(now != null, "Result is required");
    return now;
  }

  @Nullable
  @Override
  public V get() throws InterruptedException, ExecutionException {
    await();

    Throwable cause = getCause();
    if (cause == null) {
      return getNow();
    }
    if (cause instanceof CancellationException) {
      throw (CancellationException) cause;
    }
    throw new ExecutionException(cause);
  }

  @Nullable
  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (await(timeout, unit)) {
      Throwable cause = getCause();
      if (cause == null) {
        return getNow();
      }
      if (cause instanceof CancellationException) {
        throw (CancellationException) cause;
      }
      throw new ExecutionException(cause);
    }
    throw new TimeoutException("Timeout");
  }

  // listeners

  @Override
  public AbstractFuture<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    Assert.notNull(listener, "listener is required");

    synchronized(this) {
      Object local = this.listeners;
      if (local instanceof FutureListeners ls) {
        ls.add(listener);
      }
      else if (local instanceof FutureListener<?> l) {
        this.listeners = new FutureListeners(l, listener);
      }
      else {
        this.listeners = listener;
      }
    }

    if (isDone()) {
      notifyListeners();
    }

    return this;
  }

  @Override
  public <C> AbstractFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    return onCompleted(FutureListener.forAdaption(listener, context));
  }

  protected final void rethrowIfFailed() {
    Throwable cause = getCause();
    if (cause == null) {
      return;
    }
    throw ExceptionUtils.sneakyThrow(cause);
  }

  /**
   * Notify a listener that a future has completed.
   *
   * @param executor the executor to use to notify the listener {@code listener}.
   * @param future the future that is complete.
   * @param listener the listener to notify.
   */
  protected static void notifyListener(Executor executor, final Future<?> future, final FutureListener<?> listener) {
    safeExecute(executor, () -> notifyListener(future, listener));
  }

  protected final void notifyListeners() {
    safeExecute(executor, this::notifyListenersNow);
  }

  private void notifyListenersNow() {
    Object listeners;
    synchronized(this) {
      if (this.listeners == null) {
        return;
      }
      listeners = this.listeners;
      this.listeners = null;
    }
    for (; ; ) {
      if (listeners instanceof FutureListener<?> fl) {
        notifyListener(this, fl);
      }
      else if (listeners instanceof FutureListeners holder) {
        for (FutureListener<?> listener : holder.listeners) {
          notifyListener(this, listener);
        }
      }
      synchronized(this) {
        if (this.listeners == null) {
          return;
        }
        listeners = this.listeners;
        this.listeners = null;
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void notifyListener(Future future, FutureListener l) {
    try {
      l.operationComplete(future);
    }
    catch (Throwable t) {
      logger.warn("An exception was thrown by {}.operationComplete(Future)", l.getClass().getName(), t);
    }
  }

  private static void safeExecute(Executor executor, Runnable task) {
    try {
      executor.execute(task);
    }
    catch (Throwable t) {
      rejectedExecutionLogger.error(
              "Failed to submit a listener notification task. Executor shutting-down?", t);
    }
  }

}
