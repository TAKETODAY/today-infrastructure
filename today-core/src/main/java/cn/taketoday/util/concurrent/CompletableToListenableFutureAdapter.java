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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import cn.taketoday.lang.Nullable;

/**
 * Adapts a {@link CompletableFuture} or {@link CompletionStage} into a
 * {@link ListenableFuture}.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CompletableToListenableFutureAdapter<T> implements ListenableFuture<T>, BiConsumer<T, Throwable> {

  private final CompletableFuture<T> completableFuture;

  private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<>();

  /**
   * Create a new adapter for the given {@link CompletionStage}.
   */
  public CompletableToListenableFutureAdapter(CompletionStage<T> completionStage) {
    this(completionStage.toCompletableFuture());
  }

  /**
   * Create a new adapter for the given {@link CompletableFuture}.
   */
  public CompletableToListenableFutureAdapter(CompletableFuture<T> completableFuture) {
    this.completableFuture = completableFuture.whenComplete(this);
  }

  @Override
  public void accept(T result, @Nullable Throwable ex) {
    if (ex != null) {
      this.callbacks.failure(ex);
    }
    else {
      this.callbacks.success(result);
    }
  }

  @Override
  public void addCallback(ListenableFutureCallback<? super T> callback) {
    this.callbacks.addCallback(callback);
  }

  @Override
  public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
    this.callbacks.addSuccessCallback(successCallback);
    this.callbacks.addFailureCallback(failureCallback);
  }

  @Override
  public CompletableFuture<T> completable() {
    return this.completableFuture;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return this.completableFuture.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return this.completableFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    return this.completableFuture.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return this.completableFuture.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return this.completableFuture.get(timeout, unit);
  }

}
