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

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;

/**
 * {@link FutureListener} implementation which takes other {@link SettableFuture}s
 * and notifies them on completion.
 *
 * @param <V> the type of value returned by the future
 * @param <F> the type of future
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PromiseNotifier<V, F extends ListenableFuture<V>> implements FutureListener<F> {

  private static final Logger logger = LoggerFactory.getLogger(PromiseNotifier.class);

  private final SettableFuture<? super V>[] futures;

  private final boolean logNotifyFailure;

  /**
   * Create a new instance.
   *
   * @param futures the {@link SettableFuture}s to notify once this {@link FutureListener} is notified.
   */
  @SafeVarargs
  public PromiseNotifier(SettableFuture<? super V>... futures) {
    this(true, futures);
  }

  /**
   * Create a new instance.
   *
   * @param logNotifyFailure {@code true} if logging should be done in case notification fails.
   * @param futures the {@link SettableFuture}s to notify once this {@link FutureListener} is notified.
   */
  @SafeVarargs
  public PromiseNotifier(boolean logNotifyFailure, SettableFuture<? super V>... futures) {
    Assert.noNullElements(futures, "promises is required");
    this.futures = futures.clone();
    this.logNotifyFailure = logNotifyFailure;
  }

  /**
   * Link the {@link ListenableFuture} and {@link SettableFuture} such
   * that if the {@link ListenableFuture} completes the {@link SettableFuture}
   * will be notified. Cancellation is propagated both ways such
   * that if the {@link ListenableFuture} is cancelled
   * the {@link SettableFuture} is cancelled and vise-versa.
   *
   * @param future the {@link ListenableFuture} which will be
   * used to listen to for notifying the {@link SettableFuture}.
   * @param promise the {@link SettableFuture} which will be notified
   * @param <V> the type of the value.
   * @param <F> the type of the {@link ListenableFuture}
   * @return the passed in {@link ListenableFuture}
   */
  public static <V, F extends ListenableFuture<V>> F cascade(final F future, final SettableFuture<? super V> promise) {
    return cascade(true, future, promise);
  }

  /**
   * Link the {@link ListenableFuture} and {@link SettableFuture}
   * such that if the {@link ListenableFuture} completes the
   * {@link SettableFuture} will be notified. Cancellation is propagated
   * both ways such that if the {@link ListenableFuture} is cancelled
   * the {@link SettableFuture} is cancelled and vise-versa.
   *
   * @param logNotifyFailure {@code true} if logging should
   * be done in case notification fails.
   * @param future the {@link ListenableFuture} which will be
   * used to listen to for notifying the {@link SettableFuture}.
   * @param promise the {@link SettableFuture} which will be notified
   * @param <V> the type of the value.
   * @param <F> the type of the {@link ListenableFuture}
   * @return the passed in {@link ListenableFuture}
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <V, F extends ListenableFuture<V>> F cascade(
          boolean logNotifyFailure, final F future, final SettableFuture<? super V> promise) {
    promise.addListener(f -> {
      if (f.isCancelled()) {
        future.cancel(false);
      }
    });
    future.addListener(new PromiseNotifier(logNotifyFailure, promise) {
      @Override
      public void operationComplete(ListenableFuture f) throws Exception {
        if (promise.isCancelled() && f.isCancelled()) {
          // Just return if we propagate a cancel from the promise to the future and both are notified already
          return;
        }
        super.operationComplete(future);
      }
    });
    return future;
  }

  @Override
  public void operationComplete(F future) throws Exception {
    Logger internalLogger = logNotifyFailure ? logger : null;
    if (future.isSuccess()) {
      V result = future.get();
      for (SettableFuture<? super V> p : futures) {
        trySuccess(p, result, internalLogger);
      }
    }
    else if (future.isCancelled()) {
      for (SettableFuture<? super V> p : futures) {
        tryCancel(p, internalLogger);
      }
    }
    else {
      Throwable cause = future.cause();
      for (SettableFuture<? super V> p : futures) {
        tryFailure(p, cause, internalLogger);
      }
    }
  }

  /**
   * Try to cancel the {@link SettableFuture} and log if {@code logger} is not {@code null} in case this fails.
   */
  private static void tryCancel(SettableFuture<?> p, Logger logger) {
    if (!p.cancel(false) && logger != null) {
      Throwable err = p.cause();
      if (err == null) {
        logger.warn("Failed to cancel promise because it has succeeded already: {}", p);
      }
      else {
        logger.warn("Failed to cancel promise because it has failed already: {}, unnotified cause:",
                p, err);
      }
    }
  }

  /**
   * Try to mark the {@link SettableFuture} as success and log if {@code logger} is not {@code null} in case this fails.
   */
  private static <V> void trySuccess(SettableFuture<? super V> p, V result, Logger logger) {
    if (!p.trySuccess(result) && logger != null) {
      Throwable err = p.cause();
      if (err == null) {
        logger.warn("Failed to mark a promise as success because it has succeeded already: {}", p);
      }
      else {
        logger.warn("Failed to mark a promise as success because it has failed already: {}, unnotified cause:", p, err);
      }
    }
  }

  /**
   * Try to mark the {@link SettableFuture} as failure and log if {@code logger} is not {@code null} in case this fails.
   */
  private static void tryFailure(SettableFuture<?> p, Throwable cause, Logger logger) {
    if (!p.tryFailure(cause) && logger != null) {
      Throwable err = p.cause();
      if (err == null) {
        logger.warn("Failed to mark a promise as failure because it has succeeded already: {}", p, cause);
      }
      else if (logger.isWarnEnabled()) {
        logger.warn("Failed to mark a promise as failure because it has failed already: {}, unnotified cause: {}",
                p, ExceptionUtils.stackTraceToString(err), cause);
      }
    }
  }

}
