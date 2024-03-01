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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Nullable;

/**
 * Extend {@link Future} with the capability to accept completion callbacks.
 * If the future has completed when the callback is added, the callback is
 * triggered immediately.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ListenableFuture}.
 * and {@code io.netty.util.concurrent.Future}
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface ListenableFuture<T> extends Future<T> {

  /**
   * Returns {@code true} if and only if the operation was completed
   * successfully.
   */
  boolean isSuccess();

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
   */
  @Nullable
  Throwable getCause();

  /**
   * Java 8 lambda-friendly alternative with success and failure callbacks.
   *
   * @param successCallback the success callback
   * @param failureCallback the failure callback
   */
  default ListenableFuture<T> addListener(SuccessCallback<T> successCallback, @Nullable FailureCallback failureCallback) {
    return addListener(FutureListener.forAdaption(successCallback, failureCallback));
  }

  /**
   * Java 8 lambda-friendly alternative with success callbacks.
   *
   * @param successCallback the success callback
   */
  default ListenableFuture<T> onSuccess(SuccessCallback<T> successCallback) {
    return addListener(successCallback, null);
  }

  /**
   * Java 8 lambda-friendly alternative with failure callbacks.
   *
   * @param failureCallback the failure callback
   */
  default ListenableFuture<T> onFailure(FailureCallback failureCallback) {
    return addListener(FutureListener.forFailure(failureCallback));
  }

  /**
   * Adds the specified listener to this future.
   * <p>
   * The specified listener is notified when this future is
   * {@linkplain #isDone() done}. If this future is already
   * completed, the specified listener is notified immediately.
   */
  ListenableFuture<T> addListener(FutureListener<? extends ListenableFuture<T>> listener);

  /**
   * Adds the specified listeners to this future.
   * <p>
   * The specified listeners are notified when this future is
   * {@linkplain #isDone() done}.  If this future is already
   * completed, the specified listeners are notified immediately.
   */
  ListenableFuture<T> addListeners(FutureListener<? extends ListenableFuture<T>>... listeners);

  /**
   * Removes the first occurrence of the specified listener from
   * this future.
   * <p>
   * The specified listener is no longer notified when this
   * future is {@linkplain #isDone() done}.  If the specified
   * listener is not associated with this future, this method
   * does nothing and returns silently.
   */
  ListenableFuture<T> removeListener(FutureListener<? extends ListenableFuture<T>> listener);

  /**
   * Removes the first occurrence for each of the listeners from this future.
   * <p>
   * The specified listeners are no longer notified when this
   * future is {@linkplain #isDone() done}.  If the specified
   * listeners are not associated with this future, this method
   * does nothing and returns silently.
   */
  ListenableFuture<T> removeListeners(FutureListener<? extends ListenableFuture<T>>... listeners);

  /**
   * Waits for this future until it is done, and rethrows the cause of the failure if this future
   * failed.
   */
  ListenableFuture<T> sync() throws InterruptedException;

  /**
   * Waits for this future until it is done, and rethrows the cause of the failure if this future
   * failed.
   */
  ListenableFuture<T> syncUninterruptibly();

  /**
   * Waits for this future to be completed.
   *
   * @throws InterruptedException if the current thread was interrupted
   */
  ListenableFuture<T> await() throws InterruptedException;

  /**
   * Waits for this future to be completed without
   * interruption.  This method catches an {@link InterruptedException} and
   * discards it silently.
   */
  ListenableFuture<T> awaitUninterruptibly();

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
  T getNow();

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
  T obtain() throws IllegalStateException;

  /**
   * {@inheritDoc}
   *
   * If the cancellation was successful it will fail the future with a {@link CancellationException}.
   */
  @Override
  boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Expose this {@link ListenableFuture} as a JDK {@link CompletableFuture}.
   */
  default CompletableFuture<T> completable() {
    DelegatingCompletableFuture<T> completable = new DelegatingCompletableFuture<>(this);
    addListener(completable);
    return completable;
  }

  // Static Factory Methods

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
   * Creates a new FailedFuture instance.
   */
  static <V> FailedFuture<V> forFailed(Throwable cause) {
    return new FailedFuture<>(cause);
  }

  /**
   * Creates a new FailedFuture instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Future once it is complete.
   */
  static <V> FailedFuture<V> forFailed(@Nullable Executor executor, Throwable cause) {
    return new FailedFuture<>(executor, cause);
  }

  /**
   * Creates a new SucceededFuture instance.
   */
  static <V> SucceededFuture<V> forSucceeded(@Nullable V result) {
    return new SucceededFuture<>(result);
  }

  /**
   * Creates a new SucceededFuture instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the Future once it is complete.
   */
  static <V> SucceededFuture<V> forSucceeded(@Nullable Executor executor, @Nullable V result) {
    return new SucceededFuture<>(executor, result);
  }

}
