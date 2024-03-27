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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A helper to create a new {@code Future} whose result is generated from a combination
 * of input futures.
 *
 * <p>See {@link Future#whenAllComplete} and {@link Future#whenAllSucceed} for how to
 * instantiate this class.
 *
 * <p>Example:
 *
 * <pre>{@code
 * var loginDateFuture = loginService.findLastLoginDate(username);
 * var recentCommandsFuture = recentCommandsService.findRecentCommands(username);
 * var usageFuture = Future.whenAllSucceed(loginDateFuture, recentCommandsFuture)
 *     .call(() -> new UsageHistory(username, loginDateFuture.obtain(),
 *         recentCommandsFuture.obtain()), executor);
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/23 23:06
 */
@SuppressWarnings("rawtypes")
public final class FutureCombiner implements FutureContextListener<Future<?>, AbstractSettableFuture<?>> {

  private final int expectedCount;

  private final boolean allMustSucceed;

  private final Collection<Future<?>> futures;

  private final AtomicInteger done = new AtomicInteger();

  FutureCombiner(boolean allMustSucceed, Collection<Future<?>> futures) {
    Assert.notNull(futures, "futures is required");
    this.futures = futures;
    this.allMustSucceed = allMustSucceed;
    this.expectedCount = futures.size();
  }

  /**
   * Creates the {@link Future} which will return the result of calling {@link
   * Callable#call} in {@code combiner} when all futures complete, using the specified {@code
   * executor}.
   *
   * <p>If the combiner throws a {@code CancellationException}, the returned future will be
   * cancelled.
   *
   * <p>If the combiner throws an {@code ExecutionException}, the cause of the thrown {@code
   * ExecutionException} will be extracted and returned as the cause of the new {@code
   * ExecutionException} that gets thrown by the returned combined future.
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @return a future whose result is based on {@code combiner} (or based on the input futures
   * passed to {@code whenAllSucceed}, if that is the method you used to create this {@code
   * FutureCombiner}).
   */
  public <C> Future<C> call(Callable<C> combiner) {
    return call(combiner, Future.defaultExecutor);
  }

  /**
   * Creates the {@link Future} which will return the result of calling {@link
   * Callable#call} in {@code combiner} when all futures complete, using the specified {@code
   * executor}.
   *
   * <p>If the combiner throws a {@code CancellationException}, the returned future will be
   * cancelled.
   *
   * <p>If the combiner throws an {@code ExecutionException}, the cause of the thrown {@code
   * ExecutionException} will be extracted and returned as the cause of the new {@code
   * ExecutionException} that gets thrown by the returned combined future.
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param executor the {@link Executor} which is used to notify the {@code Future}
   * once it is complete. and combiner execution
   * @return a future whose result is based on {@code combiner} (or based on the input futures
   * passed to {@code whenAllSucceed}, if that is the method you used to create this {@code
   * FutureCombiner}).
   */
  @SuppressWarnings("unchecked")
  public <C> Future<C> call(Callable<C> combiner, @Nullable Executor executor) {
    var task = new ListenableFutureTask<>(executor, combiner);
    if (expectedCount == 0) {
      safeExecute(executor, task);
    }
    else {
      for (Future future : futures) {
        future.onCompleted(this, task);
      }
    }
    return task;
  }

  /**
   * Creates the {@link Future} which will return the result of running {@code combiner}
   * when all Futures complete. {@code combiner} will run using {@code executor}.
   *
   * <p>If the combiner throws a {@code CancellationException}, the returned future will be
   * cancelled.
   *
   * <p>Canceling this Future will attempt to cancel all the component futures.
   *
   * @return a future whose result is based on {@code combiner} (or based on the input futures
   * passed to {@code whenAllSucceed}, if that is the method you used to create this {@code
   * FutureCombiner}).
   */
  public Future<Void> run(Runnable combiner) {
    return run(combiner, null);
  }

  /**
   * Creates the {@link Future} which will return the result of running {@code combiner}
   * when all Futures complete. {@code combiner} will run using {@code executor}.
   *
   * <p>If the combiner throws a {@code CancellationException}, the returned future will be
   * cancelled.
   *
   * <p>Canceling this Future will attempt to cancel all the component futures.
   *
   * @param executor the {@link Executor} which is used to notify the {@code Future}
   * once it is complete. and combiner execution
   * @return a future whose result is based on {@code combiner} (or based on the input futures
   * passed to {@code whenAllSucceed}, if that is the method you used to create this {@code
   * FutureCombiner}).
   * @throws NullPointerException if task null
   */
  public Future<Void> run(Runnable combiner, @Nullable Executor executor) {
    return call(Executors.callable(combiner, null), executor);
  }

  /**
   * Creates the {@link Future} which will return Void when all Futures complete.
   *
   * <p>Canceling this Future will attempt to cancel all the component futures.
   *
   * @return a future whose result is Void
   */
  public Future<Void> combine() {
    return combine(Future.defaultExecutor);
  }

  /**
   * Creates the {@link Future} which will return Void when all Futures complete.
   *
   * <p>Canceling this Future will attempt to cancel all the component futures.
   *
   * @param executor the {@link Executor} which is used to notify
   * the {@code Future} once it is complete.
   * @return a future whose result is Void
   */
  @SuppressWarnings("unchecked")
  public Future<Void> combine(@Nullable Executor executor) {
    if (expectedCount == 0) {
      return Future.ok(null);
    }
    var settable = new DefaultFuture<Void>(executor);
    for (Future future : futures) {
      future.onCompleted(this, settable);
    }
    return settable;
  }

  @Override
  public void operationComplete(Future<?> completed, AbstractSettableFuture<?> future) throws Throwable {
    int doneCount = done.incrementAndGet();
    if (allMustSucceed) {
      Throwable cause = completed.getCause();
      if (cause != null) {
        propagateFailure(completed, future, cause);
        return;
      }
    }

    if (doneCount == expectedCount) {
      // all done
      if (future instanceof Runnable task) {
        safeExecute(future.executor(), task);
      }
      else {
        future.trySuccess(null);
      }
    }
  }

  private <C> void propagateFailure(Future<?> completed, AbstractSettableFuture<C> settable, Throwable cause) {
    // failed
    if (completed.isCancelled()) {
      settable.cancel(true);
    }
    else {
      settable.tryFailure(cause);
    }

    // cancel all tasks
    for (Future<?> future : futures) {
      if (completed != future) {
        future.cancel(true);
      }
    }
  }

  private static void safeExecute(@Nullable Executor executor, Runnable task) {
    if (executor == null) {
      executor = Future.defaultExecutor;
    }
    executor.execute(task);
  }

}
