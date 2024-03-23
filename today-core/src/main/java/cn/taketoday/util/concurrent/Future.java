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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import cn.taketoday.core.Pair;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.ThrowingBiFunction;

/**
 * The result of an asynchronous operation.
 * <p>
 * An asynchronous operation is one that might be completed outside
 * a given thread of execution. The operation can either be performing
 * computation, or I/O, or both.
 * <p>
 * A {@link Future} is either <em>uncompleted</em> or <em>completed</em>.
 * When an operation begins, a new future object is created. The new
 * future is uncompleted initially - it is neither succeeded, failed, nor
 * cancelled because the operation is not finished yet. If the operation
 * is finished either successfully, with failure, or by cancellation, the
 * future is marked as completed with more specific information, such as
 * the cause of the failure. Please note that even failure and cancellation
 * belong to the completed state.
 * <pre>
 *                                      +---------------------------+
 *                                      | Completed successfully    |
 *                                      +---------------------------+
 *                                 +---->      isDone() = true      |
 * +--------------------------+    |    |   isSuccess() = true      |
 * |        Uncompleted       |    |    +===========================+
 * +--------------------------+    |    | Completed with failure    |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * | isCancelled() = false    |    |    |    getCause() = non-null  |
 * |    getCause() = throws   |    |    +===========================+
 * |      getNow() = null     |    |    | Completed by cancellation |
 * |    isFailed() = false    |    |    +---------------------------+
 * +--------------------------+    +---->      isDone() = true      |
 *                                      | isCancelled() = true      |
 *                                      +---------------------------+
 * </pre>
 * <p>
 * Various methods are provided to let you check if the operation has been
 * completed, wait for the completion, and retrieve the result of the operation.
 * It also allows you to add {@link FutureListener}s so you can get notified
 * when the operation is completed.
 *
 * <p>
 * The {@link #onCompleted(FutureListener)} method is non-blocking. It simply
 * adds the specified {@link FutureListener} to the {@link Future}, and the
 * thread will notify the listeners when the operation associated with the future
 * is done. The {@link FutureListener} and {@link FutureContextListener}
 * callbacks yield the best performance and resource utilization because it
 * does not block at all, but it could be tricky to implement a sequential
 * logic if you are not used to event-driven programming.
 *
 * <h3>Do not confuse timeout and await timeout</h3>
 * <p>
 * The timeout value you specify with {@link #await(long, TimeUnit)} is
 * not related to the timeout at all. If an operation times out, the future
 * will be marked as 'completed with failure,' as depicted in the diagram above.
 * For example, connect timeout should be configured via a transport-specific option:
 * <pre> {@code
 * // BAD - NEVER DO THIS
 * B b = ...;
 * Future f = b.connect(...);
 * f.await(10, TimeUnit.SECONDS);
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     // You might get a NullPointerException here because the future
 *     // might not be completed yet.
 *     f.getCause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 *
 * // GOOD
 * B b = ...;
 * // Configure the connect timeout option.
 * Future f = b.connect(...);
 * f.await();
 *
 * // Now we are sure the future is completed.
 * assert f.isDone();
 *
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     f.getCause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 * }</pre>
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ListenableFuture}.
 * and {@code io.netty.util.concurrent.Future}
 *
 * @param <V> the result type returned by this Future's {@code get} method
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface Future<V> extends java.util.concurrent.Future<V> {

  /**
   * The default executor is {@link ForkJoinPool#commonPool()}.
   * <p>
   * Facts about ForkJoinPool:
   *
   * <ul>
   * <li>It is work-stealing, i.e. all threads in the pool attempt to find work
   * submitted to the pool. Especially this is efficient under heavy load
   * (many small tasks), e.g. when tasks create subtasks (recursive threads).
   * </li>
   * <li>The ForkJoinPool is dynamic, it has a maximum of 32767 running threads.
   * Compared to fixed-size pools, this reduces the risk of dead-locks.
   * </li>
   * <li>The commonPool() is shared across the entire VM. Keep this in mind when also using
   * {@link java.util.stream.Stream#parallel()} and {@link java.util.concurrent.CompletableFuture}}
   * </li>
   * </ul>
   *
   * The ForkJoinPool creates daemon threads but its run state is unaffected
   * by attempts to shutdown() or shutdownNow(). However, all running tasks are
   * immediately terminated upon program System.exit(int).
   * <p>
   * IMPORTANT: Invoke {@code ForkJoinPool.commonPool().awaitQuiescence(long, TimeUnit)}
   * before exit in order to ensure that all running async tasks complete before program termination.
   *
   * @see ForkJoinPool#awaitQuiescence(long, TimeUnit)
   */
  Executor defaultExecutor = DefaultExecutorFactory.lookup();

  /**
   * Returns {@code true} if and only if the operation was completed
   * successfully.
   */
  boolean isSuccess();

  /**
   * Returns {@code true} if and only if the operation was completed and failed.
   *
   * @see #getCause()
   */
  boolean isFailed();

  /**
   * Return {@code true} if this operation has been {@linkplain #cancel() cancelled}.
   *
   * @return {@code true} if this operation has been cancelled, otherwise {@code false}.
   */
  boolean isCancelled();

  /**
   * @return returns {@code true} if and only if the operation can
   * be cancelled via {@link #cancel(boolean)}.
   */
  boolean isCancellable();

  /**
   * Returns the cause of the failed operation if the operation failed.
   *
   * @return the cause of the failure. {@code null} if succeeded or
   * this future is not completed yet.
   * @see #isFailed()
   */
  @Nullable
  Throwable getCause();

  /**
   * Java 8 lambda-friendly alternative with success and failure callbacks.
   *
   * @param successCallback the success callback
   * @param failureCallback the failure callback
   * @return this future object.
   */
  default Future<V> onCompleted(SuccessCallback<V> successCallback, @Nullable FailureCallback failureCallback) {
    return onCompleted(FutureListener.forAdaption(successCallback, failureCallback));
  }

  /**
   * Java 8 lambda-friendly alternative with success callbacks.
   *
   * @param successCallback the success callback
   * @return this future object.
   */
  default Future<V> onSuccess(SuccessCallback<V> successCallback) {
    return onCompleted(successCallback, null);
  }

  /**
   * Java 8 lambda-friendly alternative with failure callbacks.
   *
   * @param failureCallback the failure callback
   * @return this future object.
   */
  default Future<V> onFailure(FailureCallback failureCallback) {
    return onCompleted(FutureListener.forFailure(failureCallback));
  }

  /**
   * Adds the specified listener to this future. The specified listener
   * is notified when this future is {@link #isDone() done}. If this
   * future is already completed, the specified listener is notified immediately.
   *
   * @param listener The listener to be called when this future completes.
   * The listener will be passed the given context, and this future.
   * @param context The context object that will be passed to the listener
   * when this future completes.
   * @return this future object.
   */
  default <C> Future<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    return onCompleted(FutureListener.forAdaption(listener, context));
  }

  /**
   * Adds the specified listener to this future.
   * <p>
   * The specified listener is notified when this future is
   * {@linkplain #isDone() done}. If this future is already
   * completed, the specified listener is notified immediately.
   *
   * @return this future object.
   */
  Future<V> onCompleted(FutureListener<? extends Future<V>> listener);

  /**
   * Waits for this future until it is done, and rethrows the cause of the
   * failure if this future failed.
   *
   * @return this future object.
   */
  Future<V> sync() throws InterruptedException;

  /**
   * Waits for this future until it is done, and rethrows the cause of the
   * failure if this future failed.
   *
   * @return this future object.
   */
  Future<V> syncUninterruptibly();

  /**
   * Waits for this future to be completed.
   *
   * @return this future object.
   * @throws InterruptedException if the current thread was interrupted
   */
  Future<V> await() throws InterruptedException;

  /**
   * Waits for this future to be completed without
   * interruption.  This method catches an {@link InterruptedException} and
   * discards it silently.
   *
   * @return this future object.
   */
  Future<V> awaitUninterruptibly();

  /**
   * Waits for this future to be completed within the
   * specified time limit.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   * @throws InterruptedException if the current thread was interrupted
   */
  boolean await(long timeout, TimeUnit unit) throws InterruptedException;

  /**
   * Waits for this future to be completed within the
   * specified time limit.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   * @throws InterruptedException if the current thread was interrupted
   */
  boolean await(long timeoutMillis) throws InterruptedException;

  /**
   * Waits for this future to be completed within the
   * specified time limit without interruption.  This method catches an
   * {@link InterruptedException} and discards it silently.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   */
  boolean awaitUninterruptibly(long timeout, TimeUnit unit);

  /**
   * Waits for this future to be completed within the
   * specified time limit without interruption.  This method catches an
   * {@link InterruptedException} and discards it silently.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   */
  boolean awaitUninterruptibly(long timeoutMillis);

  /**
   * Return the result without blocking. If the future is not done
   * yet this will return {@code null}.
   * <p>
   * As it is possible that a {@code null} value is used to mark
   * the future as successful you also need to check
   * if the future is really done with {@link #isDone()} and not
   * rely on the returned {@code null} value.
   */
  @Nullable
  V getNow();

  /**
   * Return the result without blocking.
   * <p>
   * must invoke after {@link #isSuccess()}
   *
   * @throws IllegalStateException {@link SettableFuture#setSuccess(Object)} is set {@code null}
   * @see #isSuccess()
   * @see #getNow()
   * @see SettableFuture#setSuccess(Object)
   */
  V obtain() throws IllegalStateException;

  /**
   * Cancel this asynchronous operation, unless it has already been
   * completed or is not {@linkplain #isCancellable() cancellable}.
   * <p>
   * A cancelled operation is considered to be {@linkplain #isDone() done}
   * and {@linkplain #isFailed() failed}.
   * <p>
   * If the cancellation was successful, the result of this operation
   * will be that it has failed with a {@link CancellationException}.
   * <p>
   * Cancellation will not cause any threads working on the operation
   * to be {@linkplain Thread#interrupt() interrupted}.
   *
   * @return {@code true} if the operation was cancelled by this call,
   * otherwise {@code false}.
   */
  default boolean cancel() {
    return cancel(true);
  }

  /**
   * {@inheritDoc}
   *
   * If the cancellation was successful it will fail the future with
   * a {@link CancellationException}.
   */
  @Override
  boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped through the given mapper function.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other. If the mapper function throws, the returned future will
   * fail, but this future will be unaffected.
   *
   * @param mapper The function that will convert the result of this
   * future into the result of the returned future.
   * @param <R> The result type of the mapper function, and of the returned future.
   * @return A new future instance that will complete with the mapped
   * result of this future.
   */
  default <R> Future<R> map(Function<V, R> mapper) {
    return Futures.map(this, mapper);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} flat-mapped through the
   * given mapper function.
   * <p>
   * The "flat" in "flat-map" means the given mapper function produces
   * a result that itself is a future-of-R, yet this method also returns
   * a future-of-R, rather than a future-of-future-of-R. In other words,
   * if the same mapper function was used with the {@link #map(Function)}
   * method, you would get back a {@code Future<Future<R>>}. These nested
   * futures are "flattened" into a {@code Future<R>} by this method.
   * <p>
   * Effectively, this method behaves similar to this serial code, except
   * asynchronously and with proper exception and cancellation handling:
   * <pre>{@code
   * V x = future.sync().getNow();
   * Future<R> y = mapper.apply(x);
   * R result = y.sync().getNow();
   * }</pre>
   * <p>
   * If the given future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel the
   * other. If the mapper function throws, the returned future will fail,
   * but this future will be unaffected.
   *
   * @param mapper The function that will convert the result of this future
   * into the result of the returned future.
   * @param <R> The result type of the mapper function, and of the returned future.
   * @return A new future instance that will complete with the mapped result
   * of this future.
   */
  default <R> Future<R> flatMap(Function<V, Future<R>> mapper) {
    return Futures.flatMap(this, mapper);
  }

  /**
   * Link the {@link Future} and {@link SettableFuture} such that if the
   * {@link Future} completes the {@link SettableFuture}
   * will be notified. Cancellation is propagated both ways such that if
   * the {@link Future} is cancelled the {@link SettableFuture} is cancelled
   * and vice-versa.
   *
   * @param settable the {@link SettableFuture} which will be notified
   * @return itself
   * @throws IllegalArgumentException SettableFuture is null.
   */
  default Future<V> cascadeTo(final SettableFuture<? super V> settable) {
    Futures.cascade(this, settable);
    return this;
  }

  /**
   * Handles a failure of this Future by returning another result.
   * <p>
   * Example:
   * <pre>{@code
   * // = "oh!"
   * Future.run(() -> new Error("oh!")).recover(Throwable::getMessage);
   * }</pre>
   *
   * @param errorHandler A function which takes the exception of a failure and returns a new value.
   * @return A new Future.
   * @throws IllegalArgumentException recoverFunc is null.
   */
  default Future<V> errorHandling(Function<Throwable, V> errorHandler) {
    Assert.notNull(errorHandler, "errorHandler is required");
    return Futures.errorHandling(this, errorHandler);
  }

  /**
   * Returns a tuple of this and that Future result.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a tuple of both successful Future results.
   *
   * @param that Another Future
   * @param <U> Result type of {@code that}
   * @return A new Future that returns both Future results.
   * @throws IllegalArgumentException if {@code that} is null
   */
  default <U> Future<Pair<V, U>> zip(Future<? extends U> that) {
    Assert.notNull(that, "Future is is required");
    return zipWith(that, Pair::of);
  }

  /**
   * Returns this and that Future result combined using a given combinator function.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a combination of both successful Future results.
   *
   * @param that Another Future
   * @param combinator The combinator function
   * @param <U> Result type of {@code that}
   * @param <R> Result type of {@code f}
   * @return A new Future that returns both Future results.
   * @throws IllegalArgumentException if {@code that} is null
   */
  default <U, R> Future<R> zipWith(Future<U> that, ThrowingBiFunction<V, U, R> combinator) {
    Assert.notNull(that, "Future is required");
    Assert.notNull(combinator, "combinator is required");
    return Futures.zipWith(this, that, combinator);
  }

  /**
   * Waits for the future to complete, then calls the given result handler
   * with the outcome.
   * <p>
   * If the future completes successfully, then the result handler is called
   * with the result of the future - which may be {@code null} - and a
   * {@code null} exception.
   * <p>
   * If the future fails, then the result handler is called with a {@code null}
   * result, and a non-{@code null} exception.
   * <p>
   * Success or failure of the future can be determined on whether the exception
   * is {@code null} or not.
   * <p>
   * The result handler may compute a new result, which will be the return value
   * of the {@code join} call.
   *
   * @param resultHandler The function that will process the result of the completed future.
   * @param <T> The return type of the {@code resultHandler}.
   * @return The result of the {@code resultHandler} computation.
   * @throws InterruptedException if the thread is interrupted while waiting for
   * the future to complete.
   */
  default <T> T join(BiFunction<V, Throwable, T> resultHandler) throws InterruptedException {
    Assert.notNull(resultHandler, "resultHandler is required");
    await();
    if (isSuccess()) {
      return resultHandler.apply(getNow(), null);
    }
    else {
      return resultHandler.apply(null, getCause());
    }
  }

  /**
   * Expose this {@link Future} as a JDK {@link CompletableFuture}.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  default CompletableFuture<V> completable() {
    final CompletableFuture<V> ret = new CompletableFuture<>();
    onCompleted((FutureContextListener) Futures.completableAdapter, ret);
    return ret;
  }

  /**
   * Returns the {@link Executor} used by this {@code Future}.
   * <p>
   * The {@link Executor} which is used to notify the {@code Future} once it is complete.
   *
   * @return The underlying {@code Executor}.
   */
  Executor executor();

  // Static Factory Methods

  /**
   * Adapts {@code CompletionStage} to a new SettableFuture instance.
   */
  static <V> SettableFuture<V> forAdaption(CompletionStage<V> stage) {
    return forAdaption(stage, defaultExecutor);
  }

  /**
   * Adapts {@code CompletionStage} to a new SettableFuture instance.
   *
   * @param executor The {@link Executor} which is used to notify the {@code Future} once it is complete.
   */
  static <V> SettableFuture<V> forAdaption(CompletionStage<V> stage, @Nullable Executor executor) {
    SettableFuture<V> settable = forSettable(executor);
    stage.thenAcceptAsync(settable::trySuccess)
            .exceptionally(failure -> {
              settable.setFailure(failure);
              return null;
            });
    return settable;
  }

  /**
   * Creates a new SettableFuture instance.
   */
  static <V> SettableFuture<V> forSettable() {
    return new DefaultFuture<>();
  }

  /**
   * Creates a new SettableFuture instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the SettableFuture once it is complete.
   */
  static <V> SettableFuture<V> forSettable(@Nullable Executor executor) {
    return new DefaultFuture<>(executor);
  }

  /**
   * Creates a failed {@code Future} with the given {@code exception},
   * backed by the {@link #defaultExecutor}.
   *
   * @param cause The reason why it failed.
   * @param <V> The value type of successful result.
   * @return A failed {@code Future}.
   * @throws IllegalArgumentException if exception is null
   */
  static <V> FailedFuture<V> failed(Throwable cause) {
    return new FailedFuture<>(cause);
  }

  /**
   * Creates a failed {@code Future} with the given {@code exception},
   * backed by the given {@link Executor}.
   *
   * @param executor The {@link Executor} which is used to notify the
   * {@code Future} once it is complete.
   * @param cause The reason why it failed.
   * @param <V> The value type of successful result.
   * @return A failed {@code Future}.
   * @throws NullPointerException if executor or exception is null
   */
  static <V> FailedFuture<V> failed(@Nullable Executor executor, Throwable cause) {
    return new FailedFuture<>(executor, cause);
  }

  /**
   * Creates a new SucceededFuture instance.
   */
  static <V> SucceededFuture<V> ok(@Nullable V result) {
    return new SucceededFuture<>(result);
  }

  /**
   * Creates a new SucceededFuture instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Future once it is complete.
   */
  static <V> SucceededFuture<V> ok(@Nullable Executor executor, @Nullable V result) {
    return new SucceededFuture<>(executor, result);
  }

  /**
   * Starts an asynchronous computation, backed by the {@link #defaultExecutor}.
   *
   * @param computation A computation.
   * @param <T> Type of the computation result.
   * @return A new Future instance.
   * @throws IllegalArgumentException if computation is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  static <T> Future<T> run(Callable<T> computation) {
    return run(computation, defaultExecutor);
  }

  /**
   * Starts an asynchronous computation, backed by the given {@link Executor}.
   *
   * @param executor The {@link Executor} which is used to notify the
   * {@code Future} once it is complete.
   * @param computation A computation.
   * @param <V> Type of the computation result.
   * @return A new Future instance.
   * @throws IllegalArgumentException computation is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  static <V> Future<V> run(Callable<V> computation, @Nullable Executor executor) {
    Assert.notNull(computation, "computation is required");
    if (executor == null) {
      executor = defaultExecutor;
    }
    var futureTask = new ListenableFutureTask<>(executor, computation);
    executor.execute(futureTask);
    return futureTask;
  }

  /**
   * Runs an asynchronous computation, backed by the {@link #defaultExecutor}.
   *
   * @param unit A unit of work.
   * @return A new Future instance which results in nothing.
   * @throws IllegalArgumentException if unit is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  static Future<Void> run(Runnable unit) {
    return run(unit, defaultExecutor);
  }

  /**
   * Starts an asynchronous computation, backed by the given {@link Executor}.
   *
   * @param executor An {@link Executor}.
   * @param task A unit of work.
   * @return A new Future instance which results in nothing.
   * @throws IllegalArgumentException unit is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  static Future<Void> run(Runnable task, @Nullable Executor executor) {
    Assert.notNull(task, "unit is required");
    if (executor == null) {
      executor = defaultExecutor;
    }
    var futureTask = new ListenableFutureTask<Void>(executor, task, null);
    executor.execute(futureTask);
    return futureTask;
  }

}
