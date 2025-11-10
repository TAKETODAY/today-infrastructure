/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import infra.lang.Assert;
import infra.util.function.ThrowingFunction;

/**
 * A helper to create a new {@code Future} whose result is generated from a combination
 * of input futures.
 *
 * <p>See  {@link Future#combine} for how to
 * instantiate this class.
 *
 * <p>Example:
 *
 * <pre>{@code
 * var loginDateFuture = loginService.findLastLoginDate(username);
 * var recentCommandsFuture = recentCommandsService.findRecentCommands(username);
 * var usageFuture = Future.combine(loginDateFuture, recentCommandsFuture)
 *     .call(() -> new UsageHistory(username, loginDateFuture.obtain(),
 *         recentCommandsFuture.obtain()), executor);
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/23 23:06
 */
@SuppressWarnings("rawtypes")
public final class FutureCombiner implements FutureContextListener<Future<?>, AbstractFuture<?>>, FutureListener<Future<?>> {

  private final int expectedCount;

  private final boolean allMustSucceed;

  private final Collection<Future<?>> futures;

  private final AtomicInteger done = new AtomicInteger();

  FutureCombiner(boolean allMustSucceed, Collection<Future<?>> futures) {
    this.futures = futures;
    this.allMustSucceed = allMustSucceed;
    this.expectedCount = futures.size();
  }

  /**
   * Creates a new {@link FutureCombiner} that processes the completed
   * futures whether they're successful.
   *
   * @since 5.0
   */
  public FutureCombiner with(Future<?> future) {
    Assert.notNull(future, "Future is required");
    LinkedList<Future<?>> futures = new LinkedList<>(this.futures);
    futures.add(future);
    return new FutureCombiner(allMustSucceed, futures);
  }

  /**
   * Creates a new {@link FutureCombiner} that processes the completed
   * futures whether they're successful.
   *
   * @since 5.0
   */
  public FutureCombiner with(Future<?>... future) {
    LinkedList<Future<?>> futures = new LinkedList<>(this.futures);
    Collections.addAll(futures, future);
    return new FutureCombiner(allMustSucceed, futures);
  }

  /**
   * Creates a new {@link FutureCombiner} that processes the completed
   * futures whether they're successful.
   *
   * @since 5.0
   */
  public FutureCombiner with(Collection<Future<?>> future) {
    Assert.notNull(future, "Futures is required");
    LinkedList<Future<?>> futures = new LinkedList<>(this.futures);
    futures.addAll(future);
    return new FutureCombiner(allMustSucceed, futures);
  }

  /**
   * Create a new FutureCombiner instance that requires all Future to succeed.
   * If any input fails, the returned Future will fail immediately.
   *
   * @return a new FutureCombiner instance
   * @since 5.0
   */
  public FutureCombiner requireAllSucceed() {
    return new FutureCombiner(true, futures);
  }

  /**
   * Creating a new FutureCombiner instance does not require that all the futures succeed.
   * Any failure of the input future is not propagated to the returned future.
   *
   * @return a new FutureCombiner instance
   * @since 5.0
   */
  public FutureCombiner acceptFailure() {
    return new FutureCombiner(false, futures);
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
   * passed to {@code combine}, if that is the method you used to create this {@code
   * FutureCombiner}).
   */
  public <C> Future<C> call(Callable<C> combiner) {
    return call(combiner, Future.defaultScheduler);
  }

  /**
   * Combines and maps multiple futures into a single future using the given mapper function.
   * Uses the default scheduler {@link Future#defaultScheduler} as the executor.
   *
   * <p>The mapper function is called with the collection of input futures once they have all completed,
   * and returns a value that will be the result of the returned future.
   *
   * <p>If any input future fails, the returned future fails immediately with that failure.
   *
   * @param <C> The type of result produced by the mapper function and returned future
   * @param mapper Function to map the collection of completed futures to a result
   * @return A future that completes with the mapped result once all input futures complete
   * @throws IllegalArgumentException if mapper is null
   * @see #call(ThrowingFunction, Executor)
   * @since 5.0
   */
  public <C> Future<C> call(ThrowingFunction<Collection<Future<?>>, C> mapper) {
    return call(mapper, Future.defaultScheduler);
  }

  /**
   * Combine the results of all futures using the given mapper function.
   * <p>
   * The mapper function is called with the collection of all futures once they complete successfully.
   * The returned future completes with the result of the mapper function.
   * <p>
   * If any of the futures fail, the returned future fails immediately with the same exception.
   *
   * @param <C> The result type returned by the mapper
   * @param mapper The function to combine the future results
   * @param executor The executor to use for notifications, or null to use the default
   * @return A new future that completes with the mapped result
   * @throws IllegalArgumentException if mapper is null
   * @since 5.0
   */
  public <C> Future<C> call(ThrowingFunction<Collection<Future<?>>, C> mapper, @Nullable Executor executor) {
    return call(() -> mapper.apply(futures), executor);
  }

  /**
   * Combines the successful results of all futures into a single list future.
   * <p>
   * The returned future completes when all input futures complete and:
   * <ul>
   *   <li>If all input futures succeed, returns a list containing all their results in order</li>
   *   <li>If any input future fails, the returned future fails immediately with that error</li>
   *   <li>If any input future is cancelled, the returned future is cancelled</li>
   * </ul>
   *
   * @param <T> The common result type of the input futures
   * @return A new future that completes with a list of all results when all input futures complete successfully
   * @throws ClassCastException if any future result cannot be cast to type T
   * @since 5.0
   */
  public <T> Future<List<T>> asList() {
    return asList(Future.defaultScheduler);
  }

  /**
   * Combines the successful results of all futures into a single list future.
   * <p>
   * The returned future completes when all input futures complete and:
   * <ul>
   *   <li>If all input futures succeed, returns a list containing all their results in order</li>
   *   <li>If any input future fails, the returned future fails immediately with that error</li>
   *   <li>If any input future is cancelled, the returned future is cancelled</li>
   * </ul>
   *
   * @param executor the {@link Executor} which is used to notify the {@code Future}
   * once it is complete. and combiner execution
   * @param <T> The common result type of the input futures
   * @return A new future that completes with a list of all results when all input futures complete successfully
   * @throws ClassCastException if any future result cannot be cast to type T
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public <T> Future<List<T>> asList(@Nullable Executor executor) {
    return call(() -> (List<T>) futures.stream().map(Future::getNow).collect(Collectors.toList()), executor);
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
   * passed to {@code combine}, if that is the method you used to create this {@code
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
   * passed to {@code combine}, if that is the method you used to create this {@code
   * FutureCombiner}).
   */
  public Future<Void> run(Runnable combiner) {
    return run(combiner, Future.defaultScheduler);
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
   * passed to {@code combine}, if that is the method you used to create this {@code
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
  public Future<Void> asVoid() {
    return asVoid(Future.defaultScheduler);
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
  public Future<Void> asVoid(@Nullable Executor executor) {
    if (expectedCount == 0) {
      return Future.ok();
    }
    var promise = new Promise<Void>(executor);
    for (Future future : futures) {
      future.onCompleted(this, promise);
    }
    promise.onCompleted((FutureListener) this);
    return promise;
  }

  @Override
  public void operationComplete(Future<?> completed) throws Throwable {
    if (completed.isFailed()) {
      if (allMustSucceed) {
        for (Future future : futures) {
          if (!future.isDone()) {
            future.cancel();
          }
        }
      }
    }
  }

  @Override
  public synchronized void operationComplete(Future<?> completed, AbstractFuture<?> future) throws Throwable {
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

  private <C> void propagateFailure(Future<?> completed, AbstractFuture<C> promise, Throwable cause) {
    // failed
    if (completed.isCancelled()) {
      promise.cancel();
    }
    else {
      promise.tryFailure(cause);
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
      executor = Future.defaultScheduler;
    }
    executor.execute(task);
  }

}
