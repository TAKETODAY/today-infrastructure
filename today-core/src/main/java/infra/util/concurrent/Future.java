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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import infra.core.Pair;
import infra.core.Triple;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.LoggerFactory;
import infra.util.ExceptionUtils;
import infra.util.function.ThrowingBiFunction;
import infra.util.function.ThrowingConsumer;
import infra.util.function.ThrowingFunction;
import infra.util.function.ThrowingRunnable;
import infra.util.function.ThrowingSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Represents the result of an asynchronous operation. This class provides methods to
 * check the completion status, retrieve the result, and register callbacks for specific
 * events such as success, failure, or cancellation.
 *
 * <p>An asynchronous operation is one that might be completed outside
 * a given thread of execution. The operation can either be performing
 * computation, or I/O, or both.
 *
 * <p>A {@link Future} is either <em>uncompleted</em> or <em>completed</em>.
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
 *                                      |    isFailed() = false     |
 *                                      |    getCause() = null      |
 *                                 +---->      isDone() = true      |
 * +--------------------------+    |    |   isSuccess() = true      |
 * |        Uncompleted       |    |    +===========================+
 * +--------------------------+    |    | Completed with failure    |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * |    isFailed() = false    |    |    |    isFailed() = true      |
 * | isCancelled() = false    |    |    |    getCause() = non-null  |
 * |    getCause() = null     |    |    +===========================+
 * |      getNow() = null     |    |    | Completed by cancellation |
 * |                          |    |    +---------------------------+
 * +--------------------------+    +---->      isDone() = true      |
 *                                      |    isFailed() = true      |
 *                                      | isCancelled() = true      |
 *                                      |    getCause() = non-null  |
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
 * <p>Example usage:
 * <pre>{@code
 * Future<String> future = performAsyncOperation();
 *
 * // Register a success callback
 * future.onSuccess(result -> {
 *   System.out.println("Operation succeeded with result: " + result);
 * });
 *
 * // Register a failure callback
 * future.onFailure(cause -> {
 *   System.err.println("Operation failed with cause: " + cause.getMessage());
 * });
 *
 * // Synchronize and handle the result
 * try {
 *   future.sync();
 *   if (future.isSuccess()) {
 *     System.out.println("Sync completed successfully.");
 *   }
 * } catch (InterruptedException e) {
 *   Thread.currentThread().interrupt();
 *   System.err.println("Thread was interrupted while waiting for the future.");
 * }
 * }</pre>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Supports checking the completion status via methods like {@link #isDone()},
 *       {@link #isSuccess()}, and {@link #isCancelled()}.</li>
 *   <li>Provides mechanisms to retrieve the cause of failure using {@link #getCause()}.</li>
 *   <li>Allows registering callbacks for various events such as success, failure,
 *       cancellation, and completion.</li>
 *   <li>Supports synchronous waiting using {@link #await()} and {@link #sync()}.</li>
 * </ul>
 *
 * <p>This class is thread-safe and can be used in multithreaded environments.
 *
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ListenableFuture}.
 * and {@code io.netty.util.concurrent.Future}
 *
 * @param <V> the result type returned by this Future's {@code get} method
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public abstract class Future<V> implements java.util.concurrent.Future<V> {

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
  public static final Scheduler defaultScheduler = Scheduler.lookup();

  /**
   * One or more listeners.
   */
  @Nullable
  private Object listeners;

  protected final Executor executor;

  protected Future(@Nullable Executor executor) {
    this.executor = executor == null ? defaultScheduler : executor;
  }

  /**
   * Returns {@code true} if and only if the operation was completed
   * successfully.
   */
  public abstract boolean isSuccess();

  /**
   * Returns {@code true} if and only if the operation was completed and failed or cancelled.
   *
   * <p>Returns {@code true} this future maybe {@link #isCancelled() cancelled}
   *
   * @see #getCause()
   * @see #isCancelled()
   */
  public abstract boolean isFailed();

  /**
   * Checks if the current operation is considered a failure.
   * An operation is deemed a failure if it has failed (as indicated
   * by {@link #isFailed()}) and has not been cancelled (as indicated
   * by {@link #isCancelled()}).
   *
   * <p>Example usage:
   * <pre>{@code
   * Future status = new OperationStatus();
   *
   * if (status.isFailure()) {
   *   System.out.println("The operation has failed.");
   * }
   * else {
   *   System.out.println("The operation is either successful or cancelled.");
   * }
   * }</pre>
   *
   * @return true if the operation has failed and was not cancelled,
   * false otherwise.
   * @see AbstractFuture#tryFailure(Throwable)
   * @since 5.0
   */
  public boolean isFailure() {
    return isFailed() && !isCancelled();
  }

  /**
   * Return {@code true} if this operation has been {@linkplain #cancel() cancelled}.
   * And {@link #getCause()} will returns {@link CancellationException}
   *
   * <p>Cancelled future is a {@link #isFailed() failed} future
   *
   * @return {@code true} if this operation has been cancelled, otherwise {@code false}.
   * @see CancellationException
   * @see #isFailed()
   */
  @Override
  public abstract boolean isCancelled();

  /**
   * Returns {@code true} if this task completed.
   * <p>
   * Completion may be due to normal termination, an exception, or
   * cancellation -- in all of these cases, this method will return
   * {@code true}.
   *
   * @return {@code true} if this task completed
   */
  @Override
  public abstract boolean isDone();

  /**
   * Returns the cause of the failed operation if the operation failed.
   *
   * <p> Returns a {@link CancellationException} if this future is cancelled
   *
   * @return the cause of the failure. {@code null} if succeeded or
   * this future is not completed yet.
   * @see #isFailed()
   * @see #isCancelled()
   * @see CancellationException
   */
  @Nullable
  public abstract Throwable getCause();

  /**
   * Java 8 lambda-friendly alternative with success and failure callbacks.
   *
   * <p> Adapts {@link AbstractFuture#trySuccess(Object)},
   * {@link Promise#setFailure(Throwable)} and
   * {@link AbstractFuture#tryFailure(Throwable)} operations
   *
   * @param successCallback the success callback
   * @param failureCallback the failure callback
   * @return this future object.
   * @see AbstractFuture#trySuccess(Object)
   * @see Promise#setFailure(Throwable)
   * @see AbstractFuture#tryFailure(Throwable)
   */
  public final Future<V> onCompleted(SuccessCallback<V> successCallback, @Nullable FailureCallback failureCallback) {
    return onCompleted(FutureListener.forAdaption(successCallback, failureCallback));
  }

  /**
   * Java 8 lambda-friendly alternative with success callbacks.
   *
   * @param successCallback the success callback
   * @return this future object.
   */
  public final Future<V> onSuccess(SuccessCallback<V> successCallback) {
    return onCompleted(successCallback, null);
  }

  /**
   * Java 8 lambda-friendly alternative with success callbacks.
   *
   * @param callback the success callback
   * @return this future object.
   * @since 5.0
   */
  public final Future<V> onSuccess(ThrowingRunnable callback) {
    Assert.notNull(callback, "successCallback is required");
    return onCompleted(future -> {
      if (future.isSuccess()) {
        callback.run();
      }
    });
  }

  /**
   * Add behavior triggered when the {@link Future} completes with
   * an error matching the given exception type.
   *
   * <p>
   * This error must non-cancelled {@link Future#isFailed() failed}
   *
   * @param failureCallback the failure callback
   * @return this future object.
   * @see Promise#setFailure(Throwable)
   * @see AbstractFuture#tryFailure(Throwable)
   * @see Future#isFailure()
   */
  public final Future<V> onFailure(FailureCallback failureCallback) {
    return onCompleted(FutureListener.forFailure(failureCallback));
  }

  /**
   * Add behavior triggered when the {@link Future} completes with
   * an error matching the given exception type.
   *
   * <p>
   * This error must non-cancelled {@link Future#isFailed() failed}
   *
   * @param exceptionType the type of exceptions to handle
   * @param failureCallback the error handler for relevant errors
   * @param <E> type of the error to handle
   * @return this {@link Future}
   * @since 5.0
   */
  public final <E extends Throwable> Future<V> onFailure(Class<E> exceptionType, ThrowingConsumer<E> failureCallback) {
    return onFailure(exceptionType::isInstance, failureCallback);
  }

  /**
   * Add behavior triggered when the {@link Future} completes with an
   * error matching the given predicate.
   *
   * <p>
   * This error must non-cancelled {@link Future#isFailed() failed}
   *
   * @param predicate the matcher for exceptions to handle
   * @param failureCallback the error handler for relevant error
   * @return this {@link Future}
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public final <E extends Throwable> Future<V> onFailure(Predicate<Throwable> predicate, ThrowingConsumer<E> failureCallback) {
    Assert.notNull(predicate, "predicate is required");
    Assert.notNull(failureCallback, "failureCallback is required");
    return onFailure(ex -> {
      if (predicate.test(ex)) {
        failureCallback.acceptWithException((E) ex);
      }
    });
  }

  /**
   * Java 8 lambda-friendly alternative with cancelled callbacks.
   *
   * @param callback the cancelled callback
   * @return this future object.
   * @see #isCancelled()
   * @since 5.0
   */
  public final Future<V> onCancelled(ThrowingRunnable callback) {
    Assert.notNull(callback, "cancelledCallback is required");
    return onCompleted(future -> {
      if (future.isCancelled()) {
        callback.run();
      }
    });
  }

  /**
   * Java 8 lambda-friendly alternative with cancelled callbacks.
   *
   * @param callback the cancelled callback
   * @return this future object.
   * @see #isCancelled()
   * @since 5.0
   */
  public final Future<V> onCancelled(FailureCallback callback) {
    Assert.notNull(callback, "cancelledCallback is required");
    return onCompleted(future -> {
      if (future.isCancelled()) {
        callback.onFailure(future.getCause());
      }
    });
  }

  /**
   * Java 8 lambda-friendly alternative with failed callbacks.
   *
   * @param failedCallback failed callback
   * @return this future object
   * @see #isFailed()
   * @see #isCancelled()
   * @since 5.0
   */
  public final Future<V> onFailed(FailureCallback failedCallback) {
    return onCompleted(FutureListener.forFailed(failedCallback));
  }

  /**
   * Java 8 lambda-friendly alternative with onCompleted callbacks.
   *
   * <p>This method like try-finally
   *
   * @param callback the onCompleted callback
   * @return this future object.
   * @see #isCancelled()
   * @see #isDone()
   * @since 5.0
   */
  public final Future<V> onFinally(ThrowingRunnable callback) {
    Assert.notNull(callback, "finallyCallback is required");
    return onCompleted(future -> callback.run());
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
  public <C> Future<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
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
  public Future<V> onCompleted(FutureListener<? extends Future<V>> listener) {
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

  /**
   * Waits for this future until it is done, and rethrows the cause of the
   * failure if this future failed.
   *
   * @return this future object.
   */
  public Future<V> sync() throws InterruptedException {
    await();
    rethrowIfFailed();
    return this;
  }

  /**
   * Waits for this future until it is done, and rethrows the cause of the
   * failure if this future failed.
   *
   * @return this future object.
   */
  public Future<V> syncUninterruptibly() {
    awaitUninterruptibly();
    rethrowIfFailed();
    return this;
  }

  /**
   * Waits for this future to be completed within the
   * specified time limit without interruption.
   *
   * <p> This method catches an {@link InterruptedException} and sneaky throws.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   */
  public boolean awaitUninterruptibly(long timeoutMillis) {
    return awaitUninterruptibly(timeoutMillis, MILLISECONDS);
  }

  /**
   * Waits for this future to be completed within the
   * specified time limit without interruption.
   *
   * <p> This method catches an {@link InterruptedException} and sneaky throws.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   */
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    try {
      return await(timeout, unit);
    }
    catch (InterruptedException e) {
      // Should not be raised at all.
      throw new InternalError();
    }
  }

  /**
   * Waits for this future to be completed.
   *
   * @return this future object.
   * @throws InterruptedException if the current thread was interrupted
   */
  public abstract Future<V> await() throws InterruptedException;

  /**
   * Waits for this future to be completed without
   * interruption.  This method catches an {@link InterruptedException} and
   * discards it silently.
   *
   * @return this future object.
   */
  public abstract Future<V> awaitUninterruptibly();

  /**
   * Waits for this future to be completed within the
   * specified time limit.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   * @throws InterruptedException if the current thread was interrupted
   */
  public abstract boolean await(long timeout, TimeUnit unit) throws InterruptedException;

  /**
   * Waits for this future to be completed within the
   * specified time limit.
   *
   * @return {@code true} if and only if the future was completed within
   * the specified time limit
   * @throws InterruptedException if the current thread was interrupted
   */
  public abstract boolean await(long timeoutMillis) throws InterruptedException;

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
  public abstract V getNow();

  /**
   * Returns the result value, if not completed returns the given valueIfAbsent.
   *
   * @param valueIfAbsent the value to return if not completed
   * @return the result value, if completed, else the given valueIfAbsent
   * @since 5.0
   */
  public V getNow(V valueIfAbsent) {
    V v = getNow();
    return v == null ? valueIfAbsent : v;
  }

  /**
   * Return the result without blocking.
   * <p>
   * must invoke after {@link #isSuccess()}
   *
   * @throws IllegalStateException {@link Promise#setSuccess(Object)} is set {@code null}
   * @see #isSuccess()
   * @see #getNow()
   * @see Promise#setSuccess(Object)
   */
  public final V obtain() throws IllegalStateException {
    V v = getNow();
    if (v == null) {
      throw new IllegalStateException("Result is required");
    }
    return v;
  }

  /**
   * Waits if necessary for the computation to complete, and then
   * retrieves its result.
   *
   * @return the computed result
   * @throws CancellationException if the computation was cancelled
   * @throws ExecutionException if the computation threw an
   * exception
   * @throws InterruptedException if the current thread was interrupted
   * while waiting
   */
  @Nullable
  @Override
  public abstract V get() throws InterruptedException, ExecutionException;

  /**
   * Waits if necessary for at most the given time for the computation
   * to complete, and then retrieves its result, if available.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return the computed result
   * @throws CancellationException if the computation was cancelled
   * @throws ExecutionException if the computation threw an
   * exception
   * @throws InterruptedException if the current thread was interrupted
   * while waiting
   * @throws TimeoutException if the wait timed out
   */
  @Nullable
  @Override
  public abstract V get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException;

  /**
   * Cancel this asynchronous operation, unless it has already been
   * completed.
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
  public boolean cancel() {
    return cancel(null, true);
  }

  /**
   * Attempts to cancel execution of this task.  This method has no
   * effect if the task is already completed or cancelled, or could
   * not be cancelled for some other reason. Otherwise, if this
   * task has not started when {@code cancel} is called, this task
   * should never run. If the task has already started, then
   * attempt to stop the task
   *
   * <p>The return value from this method does not necessarily
   * indicate whether the task is now cancelled; use {@link
   * #isCancelled}.
   *
   * <p>If the cancellation was successful it will fail the future with
   * a {@link CancellationException} if {@code cancellation} not given.
   *
   * @return {@code false} if the task could not be cancelled,
   * typically because it has already completed; {@code true}
   * otherwise. If two or more threads cause a task to be cancelled,
   * then at least one of them returns {@code true}. Implementations
   * may provide stronger guarantees.
   * @since 5.0
   */
  public boolean cancel(@Nullable Throwable cancellation) {
    return cancel(cancellation, true);
  }

  /**
   * {@inheritDoc}
   *
   * If the cancellation was successful it will fail the future with
   * a {@link CancellationException}.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return cancel(null, mayInterruptIfRunning);
  }

  /**
   * Attempts to cancel execution of this task.  This method has no
   * effect if the task is already completed or cancelled, or could
   * not be cancelled for some other reason.  Otherwise, if this
   * task has not started when {@code cancel} is called, this task
   * should never run.  If the task has already started, then the
   * {@code mayInterruptIfRunning} parameter determines whether the
   * thread executing this task (when known by the implementation)
   * is interrupted in an attempt to stop the task.
   *
   * <p>The return value from this method does not necessarily
   * indicate whether the task is now cancelled; use {@link
   * #isCancelled}.
   *
   * <p>If the cancellation was successful it will fail the future with
   * a {@link CancellationException} if {@code cancellation} not given.
   *
   * @param mayInterruptIfRunning {@code true} if the thread
   * executing this task should be interrupted (if the thread is
   * known to the implementation); otherwise, in-progress tasks are
   * allowed to complete
   * @return {@code false} if the task could not be cancelled,
   * typically because it has already completed; {@code true}
   * otherwise. If two or more threads cause a task to be cancelled,
   * then at least one of them returns {@code true}. Implementations
   * may provide stronger guarantees.
   * @since 5.0
   */
  public abstract boolean cancel(@Nullable Throwable cancellation, boolean mayInterruptIfRunning);

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
  public final <R> Future<R> map(ThrowingFunction<V, R> mapper) {
    Assert.notNull(mapper, "mapper is required");
    return Futures.map(this, mapper);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped to {@code null} result.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other.
   *
   * @return A new future instance that will complete with the mapped
   * result of this future.
   * @since 5.0
   */
  public final Future<Void> mapNull() {
    return map(v -> null);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped to {@code null} result.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other.
   *
   * @return A new future instance that will complete with the mapped
   * result of this future.
   * @since 5.0
   */
  public final Future<Void> mapNull(ThrowingConsumer<V> consumer) {
    return map(v -> {
      consumer.acceptWithException(v);
      return null;
    });
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} {@link #isCancelled() cancelled}.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of new future will cancel
   * this future.
   *
   * @param cancelledValue cancelled value
   * @return A new future instance that will complete with the mapped
   * result of this future.
   * @since 5.0
   */
  public final Future<V> switchIfCancelled(V cancelledValue) {
    return Futures.switchIfCancelled(this, () -> cancelledValue);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} {@link #isCancelled() cancelled}.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of new future will cancel
   * this future.
   *
   * @return A new future instance that will complete with the mapped
   * result of this future.
   * @since 5.0
   */
  public final Future<V> switchIfCancelled(ThrowingSupplier<V> cancelledMapper) {
    Assert.notNull(cancelledMapper, "cancelledMapper is required");
    return Futures.switchIfCancelled(this, cancelledMapper);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} {@link #isCancelled() cancelled}.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of new future will cancel
   * this future.
   *
   * @param cancelledFuture cancelled value
   * @return A new future instance that will complete with the mapped
   * result of this future.
   * @since 5.0
   */
  public final Future<V> switchIfCancelled(Future<V> cancelledFuture) {
    Assert.notNull(cancelledFuture, "cancelled Future is required");
    return Futures.switchIfCancelled(this, () -> cancelledFuture);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} {@link #isCancelled() cancelled}.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of new future will cancel
   * this future.
   *
   * @param cancelledFuture cancelled value
   * @return A new future instance that will complete with the mapped
   * result of this future.
   * @since 5.0
   */
  public final Future<V> switchIfCancelled(Supplier<Future<V>> cancelledFuture) {
    Assert.notNull(cancelledFuture, "cancelled Future Supplier is required");
    return Futures.switchIfCancelled(this, cancelledFuture);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped default value.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other.
   *
   * @param defaultValue default value
   * @return A new future instance that will complete with the default value
   * of this future.
   * @since 5.0
   */
  public final Future<V> switchIfEmpty(V defaultValue) {
    return map(value -> {
      if (value == null) {
        return defaultValue;
      }
      return value;
    });
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped default value supplier.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other.
   *
   * @param defaultValue default value supplier
   * @return A new future instance that will complete with the default value
   * supplier of this future.
   * @since 5.0
   */
  public final Future<V> switchIfEmpty(ThrowingSupplier<V> defaultValue) {
    Assert.notNull(defaultValue, "defaultValue Supplier is required");
    return map(value -> {
      if (value == null) {
        return defaultValue.getWithException();
      }
      return value;
    });
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped default value Future.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other.
   *
   * @param defaultValue default value
   * @return A new future instance that will complete with the default value Future
   * of this future.
   * @since 5.0
   */
  public final Future<V> switchIfEmpty(Future<V> defaultValue) {
    Assert.notNull(defaultValue, "defaultValue Future is required");
    return flatMap(value -> {
      if (value == null) {
        return defaultValue;
      }
      return Future.ok(value);
    });
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} mapped default value Future
   * supplier.
   * <p>
   * If this future fails, then the returned future will fail as well,
   * with the same exception. Cancellation of either future will cancel
   * the other.
   *
   * @param defaultValue default value
   * @return A new future instance that will complete with the default value Future
   * supplier of this future.
   * @since 5.0
   */
  public final Future<V> switchIfEmpty(Supplier<Future<V>> defaultValue) {
    Assert.notNull(defaultValue, "defaultValue Supplier is required");
    return flatMap(value -> {
      if (value == null) {
        return defaultValue.get();
      }
      return Future.ok(value);
    });
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} flat-mapped through the
   * given mapper function.
   * <p>
   * The "flat" in "flat-map" means the given mapper function produces
   * a result that itself is a future-of-R, yet this method also returns
   * a future-of-R, rather than a future-of-future-of-R. In other words,
   * if the same mapper function was used with the {@link #map(ThrowingFunction)}
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
  public final <R> Future<R> flatMap(ThrowingFunction<V, Future<R>> mapper) {
    Assert.notNull(mapper, "mapper is required");
    return Futures.flatMap(this, mapper);
  }

  /**
   * Creates a <strong>new</strong> {@link Future} that will complete
   * with the result of this {@link Future} flat-mapped through the
   * given mapper function.
   * <p>
   * The "flat" in "flat-map" means the given mapper function produces
   * a result that itself is a future-of-R, yet this method also returns
   * a future-of-R, rather than a future-of-future-of-R. In other words,
   * if the same mapper function was used with the {@link #map(ThrowingFunction)}
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
   * @param futureSupplier The function that will supply of the result of the returned future.
   * @param <R> The result type of the mapper function, and of the returned future.
   * @return A new future instance that will complete with the mapped result
   * of this future.
   * @since 5.0
   */
  public final <R> Future<R> flatMap(ThrowingSupplier<Future<R>> futureSupplier) {
    Assert.notNull(futureSupplier, "futureSupplier is required");
    return Futures.flatMap(this, v -> futureSupplier.get());
  }

  /**
   * Link the {@link Future} and {@link Promise} such that if the
   * {@link Future} completes the {@link Promise}
   * will be notified. Cancellation is propagated both ways such that if
   * the {@link Future} is cancelled the {@link Promise} is cancelled
   * and vice-versa.
   *
   * @param promise the {@link Promise} which will be notified
   * @return itself
   * @throws IllegalArgumentException Promise is null.
   */
  public final Future<V> cascadeTo(Promise<V> promise) {
    Assert.notNull(promise, "Promise is required");
    Futures.cascadeTo(this, promise);
    return this;
  }

  /**
   * Handles a failure of this Future by returning another result.
   * <p>
   * Example:
   * <pre>{@code
   * // = "oh!"
   * Future.run(() -> { throw new Error("oh!"); })
   *   .errorHandling(Throwable::getMessage);
   * }</pre>
   * <p>
   * {@code errorHandler} errors will propagate to next futures
   *
   * @param errorHandler A function which takes the exception to a failure and returns a new value.
   * @return A new Future.
   * @throws IllegalArgumentException errorHandler is null.
   */
  @SuppressWarnings("unchecked")
  public final Future<V> errorHandling(ThrowingFunction<Throwable, V> errorHandler) {
    return Futures.errorHandling(this, null, errorHandler, Futures.alwaysFunction);
  }

  /**
   * Handles a failure of this Future by returning another result.
   * <p>
   * Example:
   * <pre>{@code
   * // = "oh!"
   * Future.run(() -> { throw new IllegalStateException(); })
   *   .catching(IllegalStateException.class, i -> "oh!")
   *   .catching(IllegalArgumentException.class, IllegalArgumentException::getMessage);
   * }</pre>
   * <p>
   * {@code errorHandler} errors will propagate to next futures, this method like Java try-catch
   *
   * @param errorHandler A function which takes the exception to a failure and returns a new value.
   * @return A new Future.
   * @throws IllegalArgumentException errorHandler is null.
   * @see Class#isInstance(Object)
   */
  @SuppressWarnings("unchecked")
  public final <T> Future<V> catching(Class<T> exType, ThrowingFunction<T, V> errorHandler) {
    Assert.notNull(exType, "exType is required");
    return Futures.errorHandling(this, exType, errorHandler, Futures.isInstanceFunction);
  }

  /**
   * Handles a failure of this Future by returning another result.
   * <p>
   * Example:
   * <pre>{@code
   * // = "oh!"
   * Future.run(() -> { throw new RpcException(new IllegalStateException("oh!")); })
   *   .catchSpecificCause(IllegalArgumentException.class, i -> "iae")
   *   .catchSpecificCause(IllegalStateException.class, IllegalStateException::getMessage);
   * }</pre>
   * <p>
   * {@code errorHandler} errors will propagate to next futures
   *
   * @param errorHandler A function which takes the exception to a failure and returns a new value.
   * @return A new Future.
   * @throws IllegalArgumentException errorHandler is null.
   * @see ExceptionUtils#getMostSpecificCause(Throwable, Class)
   */
  @SuppressWarnings("unchecked")
  public final <T> Future<V> catchSpecificCause(Class<T> exType, ThrowingFunction<T, V> errorHandler) {
    Assert.notNull(exType, "exType is required");
    return Futures.errorHandling(this, exType, errorHandler, Futures.mostSpecificCauseFunction);
  }

  /**
   * Handles a failure of this Future by returning another result.
   * <p>
   * Example:
   * <pre>{@code
   * // = "oh!"
   * Future.run(() -> { throw new HttpException(new OtherError(new IllegalArgumentException("oh!"))); })
   *   .catchRootCause(IllegalStateException.class, i -> "IllegalStateException")
   *   .catchRootCause(IllegalArgumentException.class, IllegalArgumentException::getMessage);
   * }</pre>
   * <p>
   * {@code errorHandler} errors will propagate to next futures
   *
   * @param errorHandler A function which takes the exception to a failure and returns a new value.
   * @return A new Future.
   * @throws IllegalArgumentException errorHandler is null.
   * @see ExceptionUtils#getRootCause(Throwable)
   */
  @SuppressWarnings("unchecked")
  public final <T> Future<V> catchRootCause(Class<T> exType, ThrowingFunction<T, V> errorHandler) {
    Assert.notNull(exType, "exType is required");
    return Futures.errorHandling(this, exType, errorHandler, Futures.rootCauseFunction);
  }

  /**
   * Transform any error emitted by this {@link Future} by synchronously applying a function to it.
   *
   * @param mapper the error transforming {@link Function}
   * @return a {@link Future} that transforms source errors to other errors
   * @since 5.0
   */
  public final Future<V> onErrorMap(Function<Throwable, Throwable> mapper) {
    return onErrorResume(e -> Future.failed(mapper.apply(e), executor));
  }

  /**
   * Transform an error emitted by this {@link Future} by synchronously applying a function
   * to it if the error matches the given type. Otherwise let the error pass through.
   *
   * @param type the class of the exception type to react to
   * @param mapper the error transforming {@link Function}
   * @param <E> the error type
   * @return a {@link Future} that transforms some source errors to other errors
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public final <E extends Throwable> Future<V> onErrorMap(Class<E> type, Function<E, Throwable> mapper) {
    return onErrorMap(type::isInstance, (Function<Throwable, Throwable>) mapper);
  }

  /**
   * Transform an error emitted by this {@link Future} by synchronously applying a function
   * to it if the error matches the given predicate. Otherwise, let the error pass through.
   *
   * @param predicate the error predicate
   * @param mapper the error transforming {@link Function}
   * @return a {@link Future} that transforms some source errors to other errors
   * @since 5.0
   */
  public final Future<V> onErrorMap(Predicate<Throwable> predicate, Function<Throwable, Throwable> mapper) {
    return onErrorResume(predicate, e -> Future.failed(mapper.apply(e), executor));
  }

  /**
   * Given a fallback Future when an error matching the given type
   * occurs, using a function to choose the fallback depending on the error.
   *
   * @param type the error type to match
   * @param fallback the function to choose the fallback to an alternative {@link Future}
   * @param <E> the error type
   * @return a {@link Future} falling back upon source onError
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public final <E extends Throwable> Future<V> onErrorResume(Class<E> type, Function<E, Future<V>> fallback) {
    Assert.notNull(type, "type is required");
    return onErrorResume(type::isInstance, (Function<Throwable, Future<V>>) fallback);
  }

  /**
   * Given a fallback Future when an error matching a given predicate occurs.
   *
   * @param predicate the error predicate to match
   * @param fallback the function to choose the fallback to an alternative {@link Future}
   * @return a {@link Future} falling back upon source onError
   * @since 5.0
   */
  public final Future<V> onErrorResume(Predicate<Throwable> predicate, Function<Throwable, Future<V>> fallback) {
    Assert.notNull(predicate, "predicate is required");
    return onErrorResume(e -> predicate.test(e) ? fallback.apply(e) : failed(e, executor));
  }

  /**
   * Given a fallback Future when any error occurs, using a function to
   * choose the fallback depending on the error.
   *
   * @param fallback the function to choose the fallback to an alternative {@link Future}
   * @return a {@link Future} falling back upon source onError
   * @since 5.0
   */
  public final Future<V> onErrorResume(Function<Throwable, Future<V>> fallback) {
    return Futures.onErrorResume(this, fallback);
  }

  /**
   * Simply complete the Future by replacing an {@link #isFailure() failure signal}
   * with an {@link #isSuccess() null-result}. All other signals are propagated as-is.
   *
   * @return a new {@link Future} falling back on completion when an onError occurs
   * @see #onErrorReturn(Object)
   * @since 5.0
   */
  public final Future<V> onErrorComplete() {
    return onErrorReturn((Predicate<Throwable>) null, null);
  }

  /**
   * Simply complete the Future by replacing an {@link #isFailure() failure signal}
   * with an {@link #isSuccess() null-result} if the error matches the given
   * {@link Class}. All other signals, including non-matching failure, are propagated as-is.
   *
   * @return a new {@link Future} falling back on completion when a matching error occurs
   * @see #onErrorReturn(Class, Object)
   * @since 5.0
   */
  public final Future<V> onErrorComplete(Class<? extends Throwable> type) {
    Assert.notNull(type, "type is required");
    return onErrorComplete(type::isInstance);
  }

  /**
   * Simply complete the Future by replacing an {@link #isFailure() failure signal}
   * with an {@link #isSuccess() null-result} if the error matches the given
   * {@link Predicate}. All other signals, including non-matching failure, are propagated as-is.
   *
   * @return a new {@link Future} falling back on completion when a matching error occurs
   * @see #onErrorReturn(Predicate, Object)
   * @since 5.0
   */
  public final Future<V> onErrorComplete(Predicate<Throwable> predicate) {
    Assert.notNull(predicate, "predicate is required");
    return onErrorReturn(predicate, null);
  }

  /**
   * Simply emit a captured fallback value when any error is observed on this {@link Future}.
   *
   * @param fallbackValue the value to emit if an error occurs
   * @return a new falling back {@link Future}
   * @see #onErrorComplete()
   * @since 5.0
   */
  public final Future<V> onErrorReturn(@Nullable V fallbackValue) {
    return onErrorReturn((Predicate<Throwable>) null, fallbackValue);
  }

  /**
   * Simply emit a captured fallback value when an error of the specified type is
   * observed on this {@link Future}.
   *
   * @param type the error type to match
   * @param fallbackValue the value to emit if an error occurs that matches the type
   * @return a new falling back {@link Future}
   * @see #onErrorComplete(Class)
   * @since 5.0
   */
  public final Future<V> onErrorReturn(Class<? extends Throwable> type, @Nullable V fallbackValue) {
    Assert.notNull(type, "type is required");
    return onErrorReturn(type::isInstance, fallbackValue);
  }

  /**
   * Simply emit a captured fallback value when an error matching the given predicate is
   * observed on this {@link Future}.
   *
   * @param predicate the error predicate to match o null predicate indicates that all matches
   * @param fallbackValue the value to emit if an error occurs that matches the predicate
   * @return a new {@link Future}
   * @see #onErrorComplete(Predicate)
   * @since 5.0
   */
  public final Future<V> onErrorReturn(@Nullable Predicate<Throwable> predicate, @Nullable V fallbackValue) {
    return errorHandling(param -> {
      if (predicate == null || predicate.test(param)) {
        return fallbackValue;
      }
      throw param;
    });
  }

  /**
   * Returns a Pair of this and that Future result.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a tuple of both successful Future results.
   *
   * @param that Another Future
   * @param <U> Result type of {@code that}
   * @return A new Future that returns both Future results.
   * @throws IllegalArgumentException if {@code that} is null
   */
  public final <U> Future<Pair<V, U>> zip(Future<U> that) {
    return zipWith(that, Pair::of);
  }

  /**
   * Returns a Pair of this and that Future result.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a tuple of both successful Future results.
   *
   * @param that Another value
   * @param <U> Result type of {@code that}
   * @return A new Future that returns both Future results.
   * @since 5.0
   */
  public final <U> Future<Pair<V, U>> zip(U that) {
    return map(first -> Pair.of(first, that));
  }

  /**
   * Returns a Triple of this and that Future result.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a tuple of both successful Future results.
   *
   * @return A new Future that returns both Future results.
   * @throws IllegalArgumentException if {@code that} is null
   * @throws NullPointerException if {@code thatA} is null
   */
  public final <A, B> Future<Triple<V, A, B>> zip(Future<A> thatA, Future<B> thatB) {
    return zipWith(thatA.zip(thatB), (v, ab) -> Triple.of(v, ab.first, ab.second));
  }

  /**
   * Returns a Triple of this and that Future result.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a tuple of both successful Future results.
   *
   * @return A new Future that returns both Future results.
   * @since 5.0
   */
  public final <A, B> Future<Triple<V, A, B>> zip(A thatA, B thatB) {
    return map(first -> Triple.of(first, thatA, thatB));
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
  public final <U, R> Future<R> zipWith(Future<U> that, ThrowingBiFunction<V, U, R> combinator) {
    Assert.notNull(that, "Future is required");
    Assert.notNull(combinator, "combinator is required");
    return Futures.zipWith(this, that, combinator);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * <p> use {@link #defaultScheduler} for timeout checking
   *
   * @param duration timeout duration
   * @return a timeout future
   * @see TimeoutException
   * @see Scheduler
   * @since 5.0
   */
  public final Future<V> timeout(Duration duration) {
    return timeout(duration, scheduler());
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param duration timeout duration
   * @param scheduled The executor service to enforce the timeout.
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(Duration duration, ScheduledExecutorService scheduled) {
    Scheduler scheduler = createScheduler(scheduled);
    return timeout(duration, scheduler);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param duration timeout duration
   * @return a timeout future
   * @see TimeoutException
   * @see Scheduler
   * @since 5.0
   */
  public final Future<V> timeout(Duration duration, Scheduler scheduler) {
    Assert.notNull(duration, "Duration is required");
    Assert.notNull(scheduler, "Scheduler is required");
    return Futures.timeout(this, duration.toNanos(), NANOSECONDS, scheduler);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param timeout when to time out the future
   * @param unit the time unit of the time parameter
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(long timeout, TimeUnit unit) {
    return timeout(timeout, unit, scheduler());
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param timeout when to time out the future
   * @param unit the time unit of the time parameter
   * @param scheduled The executor service to enforce the timeout.
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(long timeout, TimeUnit unit, ScheduledExecutorService scheduled) {
    Scheduler scheduler = createScheduler(scheduled);
    return timeout(timeout, unit, scheduler);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param timeout when to time out the future
   * @param unit the time unit of the time parameter
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
    Assert.notNull(unit, "TimeUnit is required");
    Assert.notNull(scheduler, "Scheduler is required");
    return Futures.timeout(this, timeout, unit, scheduler);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param duration timeout duration
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(Duration duration, FutureListener<Promise<V>> timeoutListener) {
    return timeout(duration, scheduler(), timeoutListener);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param duration timeout duration
   * @param scheduled The executor service to enforce the timeout.
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(Duration duration, ScheduledExecutorService scheduled, FutureListener<Promise<V>> timeoutListener) {
    Scheduler scheduler = createScheduler(scheduled);
    return timeout(duration, scheduler, timeoutListener);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException}) if the specified duration expires.
   * <p>This future is interrupted and cancelled if it times out.
   *
   * @param duration timeout duration
   * @param scheduler for timeout checking
   * @return a timeout future
   * @see TimeoutException
   * @since 5.0
   */
  public final Future<V> timeout(Duration duration, Scheduler scheduler, FutureListener<Promise<V>> timeoutListener) {
    Assert.notNull(duration, "Duration is required");
    Assert.notNull(scheduler, "Scheduler is required");
    Assert.notNull(timeoutListener, "timeoutListener is required");
    return Futures.timeout(this, duration.toNanos(), NANOSECONDS, scheduler, timeoutListener);
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
  @Nullable
  public final <T> T join(ThrowingBiFunction<V, Throwable, T> resultHandler) throws Throwable {
    Assert.notNull(resultHandler, "resultHandler is required");
    await();
    if (isSuccess()) {
      return resultHandler.applyWithException(getNow(), null);
    }
    else {
      return resultHandler.applyWithException(null, getCause());
    }
  }

  /**
   * Waits for the future to complete.
   * <p>
   * If the future completes successfully, then returns result
   * <p>
   * If the future fails, sneaky throw any exception
   *
   * @return The result.
   * @see ExceptionUtils#sneakyThrow(Throwable)
   * @since 5.0
   */
  @Nullable
  public final V join() {
    syncUninterruptibly();
    return getNow();
  }

  /**
   * Waits for the future to complete.
   * <p>
   * If the future completes successfully, then returns result
   * <p>
   * If the future fails, sneaky throw any exception
   *
   * @param timeout timeout
   * @return The result.
   * @throws TimeoutException timeout
   * @see ExceptionUtils#sneakyThrow(Throwable)
   * @since 5.0
   */
  @Nullable
  public final V join(Duration timeout) throws TimeoutException {
    return join(timeout, false);
  }

  /**
   * Waits for the future to complete.
   * <p>
   * If the future completes successfully, then returns result
   * <p>
   * If the future fails, sneaky throw any exception
   *
   * @param timeout timeout
   * @param cancelOnTimeout invoke {@link #cancel()} when timeout
   * @return The result.
   * @throws TimeoutException timeout
   * @since 5.0
   */
  @Nullable
  public final V join(Duration timeout, boolean cancelOnTimeout) throws TimeoutException {
    if (!isDone()) {
      try {
        if (!await(timeout.toNanos(), NANOSECONDS)) {
          if (cancelOnTimeout) {
            cancel();
          }
          throw new TimeoutException("Timeout on blocking read for %s ms".formatted(timeout.toMillis()));
        }
      }
      catch (InterruptedException ex) {
        cancel();
        ex.addSuppressed(new Exception("#join(timeout) has been interrupted"));
        Thread.currentThread().interrupt();
        throw ExceptionUtils.sneakyThrow(ex);
      }
    }
    rethrowIfFailed();
    return getNow();
  }

  /**
   * Waits for the future to complete.
   * <p>
   * If the future completes successfully, then returns result
   * <p>
   * If the future fails, sneaky throw any exception
   *
   * @return The result.
   * @see ExceptionUtils#sneakyThrow(Throwable)
   * @since 5.0
   */
  public final Optional<V> block() {
    return Optional.ofNullable(join());
  }

  /**
   * Waits for the future to complete.
   * <p>
   * If the future completes successfully, then returns result
   * <p>
   * If the future fails, sneaky throw any exception
   *
   * @param timeout timeout
   * @return The result.
   * @throws TimeoutException timeout
   * @see ExceptionUtils#sneakyThrow(Throwable)
   * @since 5.0
   */
  public final Optional<V> block(Duration timeout) throws TimeoutException {
    return block(timeout, false);
  }

  /**
   * Waits for the future to complete.
   * <p>
   * If the future completes successfully, then returns result
   * <p>
   * If the future fails, sneaky throw any exception
   *
   * @param timeout timeout
   * @param cancelOnTimeout invoke {@link #cancel()} when timeout
   * @return The result.
   * @throws TimeoutException timeout
   * @since 5.0
   */
  public final Optional<V> block(Duration timeout, boolean cancelOnTimeout) throws TimeoutException {
    return Optional.ofNullable(join(timeout, cancelOnTimeout));
  }

  /**
   * Expose this {@link Future} as a JDK {@link CompletableFuture}.
   */
  @SuppressWarnings("unchecked")
  public CompletableFuture<V> completable() {
    final CompletableFuture<V> ret = new CompletableFuture<>();
    onCompleted(Futures.completableAdapter, ret);
    return ret;
  }

  /**
   * Returns the {@link Executor} used by this {@code Future}.
   * <p>
   * The {@link Executor} which is used to notify the {@code Future} once it is complete.
   *
   * @return The underlying {@code Executor}.
   */
  public Executor executor() {
    return executor;
  }

  private void rethrowIfFailed() {
    Throwable cause = getCause();
    if (cause != null) {
      throw ExceptionUtils.sneakyThrow(cause);
    }
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
    safeExecute(executor, new NotifyTask());
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
    while (true) {
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
  static void notifyListener(Future future, FutureListener l) {
    try {
      l.operationComplete(future);
    }
    catch (Throwable t) {
      LoggerFactory.getLogger(Future.class)
              .warn("An exception was thrown by {}.operationComplete(Future)", l.getClass().getName(), t);
    }
  }

  private static void safeExecute(Executor executor, Runnable task) {
    try {
      executor.execute(task);
    }
    catch (Throwable t) {
      LoggerFactory.getLogger(Future.class)
              .error("Failed to submit a listener notification task. Executor shutting-down?", t);
    }
  }

  /**
   * @since 5.0
   */
  private Scheduler createScheduler(ScheduledExecutorService scheduledService) {
    Assert.notNull(scheduledService, "ScheduledExecutorService is required");
    return new Scheduler() {

      @Override
      public void execute(Runnable command) {
        executor.execute(command);
      }

      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledService.schedule(command, delay, unit);
      }
    };
  }

  /**
   * @since 5.0
   */
  private Scheduler scheduler() {
    return executor instanceof Scheduler ? (Scheduler) executor : new Scheduler() {

      @Override
      public void execute(Runnable command) {
        executor.execute(command);
      }

      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return defaultScheduler.schedule(command, delay, unit);
      }
    };
  }

  //---------------------------------------------------------------------
  // Static Factory Methods
  //---------------------------------------------------------------------

  /**
   * Create a new async result which exposes the given value
   * from {@link Future#get()}.
   *
   * @see Future#get()
   * @since 5.0
   */
  @SuppressWarnings({ "unchecked" })
  public static <V> Future<V> ok() {
    return Futures.okFuture;
  }

  /**
   * Create a new async result which exposes the given value
   * from {@link Future#get()}.
   *
   * @param result the value to expose
   * @see Future#get()
   */
  public static <V> Future<V> ok(@Nullable V result) {
    return new CompleteFuture<>(defaultScheduler, result, null);
  }

  /**
   * Create a new async result which exposes the given value
   * from {@link Future#get()}.
   *
   * @param result the value to expose
   * @param executor the {@link Executor} which is used to notify
   * the Future once it is complete.
   * @see Future#get()
   */
  public static <V> Future<V> ok(@Nullable V result, @Nullable Executor executor) {
    return new CompleteFuture<>(executor, result, null);
  }

  /**
   * Create a new {@code null} async result which exposes the given value
   * from {@link Future#get()}.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Future once it is complete.
   * @see Future#get()
   * @since 5.0
   */
  public static <V> Future<V> forExecutor(@Nullable Executor executor) {
    return new CompleteFuture<>(executor, null, null);
  }

  /**
   * Creates a failed {@code Future} with the given {@code exception},
   * backed by the given {@link Executor}.
   *
   * @param cause The reason why it failed. the exception to expose
   * (either an pre-built {@link ExecutionException} or a cause to
   * be wrapped in an {@link ExecutionException})
   * @param <V> The value type of successful result.
   * @return A failed {@code Future}.
   * @throws NullPointerException if cause is null
   * @see ExecutionException
   */
  public static <V> Future<V> failed(Throwable cause) {
    return failed(cause, defaultScheduler);
  }

  /**
   * Creates a failed {@code Future} with the given {@code exception},
   * backed by the given {@link Executor}.
   *
   * @param executor The {@link Executor} which is used to notify the
   * {@code Future} once it is complete.
   * @param cause The reason why it failed. the exception to expose
   * (either an pre-built {@link ExecutionException} or a cause to
   * be wrapped in an {@link ExecutionException})
   * @param <V> The value type of successful result.
   * @return A failed {@code Future}.
   * @throws NullPointerException if cause is null
   * @see ExecutionException
   */
  public static <V> Future<V> failed(Throwable cause, @Nullable Executor executor) {
    Assert.notNull(cause, "cause is required");
    return new CompleteFuture<>(executor, null, cause);
  }

  /**
   * Adapts {@code CompletionStage} to a new Promise instance.
   */
  public static <V> Future<V> forAdaption(CompletionStage<V> stage) {
    return forAdaption(stage, defaultScheduler);
  }

  /**
   * Adapts {@code CompletionStage} to a new Promise instance.
   *
   * @param executor The {@link Executor} which is used to notify the
   * {@code Future} once it is complete.
   */
  @SuppressWarnings({ "unchecked" })
  public static <V> Future<V> forAdaption(CompletionStage<V> stage, @Nullable Executor executor) {
    return create(promise -> {
      stage.whenCompleteAsync((v, failure) -> {
        if (failure != null) {
          promise.tryFailure(failure);
        }
        else {
          promise.trySuccess(v);
        }
      }, promise.executor());

      promise.onCompleted(Futures.propagateCancel, stage);
    }, executor);
  }

  /**
   * Creates a new Promise instance.
   *
   * @throws NullPointerException consumer is null
   */
  public static <V> Promise<V> create(Consumer<Promise<V>> consumer) {
    return create(consumer, defaultScheduler);
  }

  /**
   * Creates a new Promise instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Promise once it is complete.
   * @throws NullPointerException consumer is null
   */
  public static <V> Promise<V> create(Consumer<Promise<V>> consumer, @Nullable Executor executor) {
    Promise<V> promise = new Promise<>(executor);
    consumer.accept(promise);
    return promise;
  }

  /**
   * Creates a new Promise instance.
   */
  public static <V> Promise<V> forPromise() {
    return new Promise<>(defaultScheduler);
  }

  /**
   * Creates a new Promise instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Promise once it is complete.
   */
  public static <V> Promise<V> forPromise(@Nullable Executor executor) {
    return new Promise<>(executor);
  }

  /**
   * Creates a new ListenableFutureTask instance.
   * like JDK {@link java.util.concurrent.FutureTask}
   *
   * @param task the callable task
   * @throws NullPointerException if computation Callable is null
   * @see java.util.concurrent.FutureTask
   */
  public static <V> ListenableFutureTask<V> forFutureTask(Callable<V> task) {
    return new ListenableFutureTask<>(defaultScheduler, task);
  }

  /**
   * Creates a new ListenableFutureTask instance.
   * like JDK {@link java.util.concurrent.FutureTask}
   *
   * @param task the callable task
   * @param executor the {@link Executor} which is used to notify
   * the ListenableFutureTask once it is complete.
   * @throws NullPointerException if computation Callable is null
   * @see java.util.concurrent.FutureTask
   */
  public static <V> ListenableFutureTask<V> forFutureTask(Callable<V> task, @Nullable Executor executor) {
    return new ListenableFutureTask<>(executor, task);
  }

  /**
   * Creates a new ListenableFutureTask instance.
   * like JDK {@link java.util.concurrent.FutureTask}
   *
   * @throws NullPointerException if task Runnable is null
   * @see java.util.concurrent.FutureTask
   */
  public static <V> ListenableFutureTask<V> forFutureTask(Runnable task) {
    return new ListenableFutureTask<>(defaultScheduler, Executors.callable(task, null));
  }

  /**
   * Creates a new ListenableFutureTask instance.
   * like JDK {@link java.util.concurrent.FutureTask}
   *
   * @param executor the {@link Executor} which is used to notify
   * the ListenableFutureTask once it is complete.
   * @throws NullPointerException if task Runnable is null
   * @see java.util.concurrent.FutureTask
   */
  public static <V> ListenableFutureTask<V> forFutureTask(Runnable task, @Nullable Executor executor) {
    return new ListenableFutureTask<>(executor, Executors.callable(task, null));
  }

  /**
   * Creates a new ListenableFutureTask instance.
   * like JDK {@link java.util.concurrent.FutureTask}
   *
   * @throws NullPointerException if task Runnable is null
   * @see java.util.concurrent.FutureTask
   */
  public static <V> ListenableFutureTask<V> forFutureTask(Runnable task, @Nullable V result) {
    return new ListenableFutureTask<>(defaultScheduler, Executors.callable(task, result));
  }

  /**
   * Creates a new ListenableFutureTask instance.
   * like JDK {@link java.util.concurrent.FutureTask}
   *
   * @param executor the {@link Executor} which is used to notify
   * the ListenableFutureTask once it is complete.
   * @throws NullPointerException if task Runnable is null
   * @see java.util.concurrent.FutureTask
   */
  public static <V> ListenableFutureTask<V> forFutureTask(Runnable task, @Nullable V result, @Nullable Executor executor) {
    return new ListenableFutureTask<>(executor, Executors.callable(task, result));
  }

  /**
   * Starts an asynchronous computation, backed by the {@link #defaultScheduler}.
   *
   * @param task A computation task.
   * @param <V> Type of the computation result.
   * @return A new Future instance.
   * @throws IllegalArgumentException if computation is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  public static <V> ListenableFutureTask<V> run(Callable<V> task) {
    return run(task, defaultScheduler);
  }

  /**
   * Starts an asynchronous computation, backed by the given {@link Executor}.
   *
   * @param executor The {@link Executor} which is used to notify the
   * {@code Future} once it is complete.
   * @param task A computation task.
   * @param <V> Type of the computation result.
   * @return A new Future instance.
   * @throws IllegalArgumentException computation is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  public static <V> ListenableFutureTask<V> run(Callable<V> task, @Nullable Executor executor) {
    return new ListenableFutureTask<>(executor, task).execute();
  }

  /**
   * Runs an asynchronous computation, backed by the {@link #defaultScheduler}.
   *
   * @param task A unit of work.
   * @return A new Future instance which results in nothing.
   * @throws IllegalArgumentException if task is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  public static ListenableFutureTask<Void> run(Runnable task) {
    return run(task, defaultScheduler);
  }

  /**
   * Starts an asynchronous computation, backed by the given {@link Executor}.
   *
   * @param executor An {@link Executor}.
   * @param task A unit of work.
   * @return A new Future instance which results in nothing.
   * @throws IllegalArgumentException task is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  public static ListenableFutureTask<Void> run(Runnable task, @Nullable Executor executor) {
    return run(task, null, executor);
  }

  /**
   * Runs an asynchronous computation, backed by the {@link #defaultScheduler}.
   *
   * @param task A unit of work.
   * @return A new Future instance which results in nothing.
   * @throws IllegalArgumentException if task is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  public static <V> ListenableFutureTask<V> run(Runnable task, @Nullable V result) {
    return run(task, result, defaultScheduler);
  }

  /**
   * Starts an asynchronous computation, backed by the given {@link Executor}.
   *
   * @param executor An {@link Executor}.
   * @param task A unit of work.
   * @return A new Future instance which results in nothing.
   * @throws IllegalArgumentException task is null.
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   */
  public static <V> ListenableFutureTask<V> run(Runnable task, @Nullable V result, @Nullable Executor executor) {
    Assert.notNull(task, "task is required");
    return forFutureTask(task, result, executor).execute();
  }

  /**
   * Creates a {@link FutureCombiner} requiring that all passed in futures are successful.
   *
   * <p>If any input fails, the returned future fails immediately.
   */
  public static FutureCombiner combine(Future<?>... futures) {
    return new FutureCombiner(true, List.of(futures));
  }

  /**
   * Creates a {@link FutureCombiner} requiring that all passed in futures are successful.
   *
   * <p>If any input fails, the returned future fails immediately.
   *
   * @param futures a collection of {@link Future}
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static FutureCombiner combine(Collection /*<Future<?>>*/ futures) {
    Assert.notNull(futures, "Futures is required");
    return new FutureCombiner(true, futures);
  }

  /**
   * Creates a {@link FutureCombiner} requiring that all passed in futures are successful.
   *
   * <p>If any input fails, the returned future fails immediately.
   *
   * @param futures a stream of {@link Future}
   * @since 5.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static FutureCombiner combine(Stream /*<Future<?>>*/ futures) {
    Assert.notNull(futures, "Futures is required");
    return new FutureCombiner(true, futures.toList());
  }

  private final class NotifyTask implements Runnable {

    @Override
    public void run() {
      notifyListenersNow();
    }
  }
}
