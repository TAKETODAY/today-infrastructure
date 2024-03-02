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

import java.util.concurrent.Executor;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * <p>A SettableFuture combiner monitors the outcome of a number of discrete
 * futures, then notifies a final, aggregate SettableFuture when all of the
 * combined futures are finished. The aggregate SettableFuture will succeed
 * if and only if all of the combined futures succeed. If any of the
 * combined futures fail, the aggregate SettableFuture will fail. The cause
 * failure for the aggregate SettableFuture will be the failure for one of
 * the failed combined futures; if more than one of the combined
 * futures fails, exactly which cause of failure will be assigned to
 * the aggregate SettableFuture is undefined.</p>
 *
 * <p>Callers may populate a SettableFuture combiner with any number of futures
 * to be combined via the {@link SettableFutureAggregator#add(ListenableFuture)}
 * and {@link SettableFutureAggregator#addAll(ListenableFuture[])} methods.
 * When all futures to be combined have been added, callers must provide
 * an aggregate SettableFuture to be notified when all combined SettableFutures have
 * finished via the {@link SettableFutureAggregator#finish(SettableFuture)} method.
 *
 * <p>This implementation is <strong>NOT</strong> thread-safe and all
 * methods must be called from the {@link Executor} thread.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class SettableFutureAggregator implements FutureListener<ListenableFuture<?>> {

  private int expectedCount;

  private int doneCount;

  @Nullable
  private Throwable cause;

  @Nullable
  private SettableFuture<Void> aggregateFuture;

  @Nullable
  private final Executor executor;

  public SettableFutureAggregator() {
    this(null);
  }

  /**
   * The {@link Executor} to use for notifications. You must call
   * {@link #add(ListenableFuture)}, {@link #addAll(ListenableFuture[])}
   * and {@link #finish(SettableFuture)} from within the {@link Executor} thread.
   *
   * @param executor the {@link Executor} to use for notifications.
   */
  public SettableFutureAggregator(@Nullable Executor executor) {
    this.executor = executor;
  }

  /**
   * Adds a new future to be combined. New futures may be added until an
   * aggregate SettableFuture is added via the
   * {@link #finish(SettableFuture)} method.
   *
   * @param future the future to add to this SettableFuture combiner
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void add(ListenableFuture future) {
    checkAddAllowed();
    ++expectedCount;
    future.addListener(this);
  }

  /**
   * Adds new futures to be combined. New futures may be added until an aggregate SettableFuture is added via the
   * {@link #finish(SettableFuture)} method.
   *
   * @param futures the futures to add to this SettableFuture combiner
   */
  @SuppressWarnings({ "rawtypes" })
  public void addAll(ListenableFuture... futures) {
    for (ListenableFuture future : futures) {
      add(future);
    }
  }

  /**
   * <p>Sets the SettableFuture to be notified when all combined futures have
   * finished. If all combined futures succeed, then the aggregate
   * SettableFuture will succeed. If one or more combined futures fails, then
   * the aggregate SettableFuture will fail with the cause of one of the failed
   * futures. If more than one combined future fails, then exactly which
   * failure will be assigned to the aggregate SettableFuture is undefined.
   *
   * <p>After this method is called, no more futures may be added via
   * the {@link #add(ListenableFuture)} or
   * {@link #addAll(ListenableFuture[])} methods.</p>
   *
   * @param aggregateFuture the SettableFuture to notify when all combined
   * futures have finished
   */
  public void finish(SettableFuture<Void> aggregateFuture) {
    Assert.notNull(aggregateFuture, "aggregateFuture is required");
    if (this.aggregateFuture != null) {
      throw new IllegalStateException("Already finished");
    }
    this.aggregateFuture = aggregateFuture;
    if (doneCount == expectedCount) {
      trySettableFuture(aggregateFuture);
    }
  }

  private boolean trySettableFuture(SettableFuture<Void> aggregateFuture) {
    return cause == null ? aggregateFuture.trySuccess(null) : aggregateFuture.tryFailure(cause);
  }

  private void checkAddAllowed() {
    if (aggregateFuture != null) {
      throw new IllegalStateException("Adding SettableFutures is not allowed after finished adding");
    }
  }

  @Override
  public void operationComplete(final ListenableFuture<?> future) {
    Executor executor = this.executor;
    if (executor == null) {
      executor = DefaultFuture.defaultExecutor;
    }

    executor.execute(() -> {
      ++doneCount;
      if (!future.isSuccess() && cause == null) {
        cause = future.getCause();
      }
      if (doneCount == expectedCount && aggregateFuture != null) {
        trySettableFuture(aggregateFuture);
      }
    });
  }

}
