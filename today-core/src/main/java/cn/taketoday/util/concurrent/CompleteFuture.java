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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A skeletal {@link Future} implementation which represents
 * a {@link Future} which has been completed already.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:28
 */
final class CompleteFuture<V> extends Future<V> {

  @Nullable
  private final V value;

  @Nullable
  private final Throwable executionException;

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} associated with this future
   * @param value the value to pass through
   */
  CompleteFuture(@Nullable Executor executor, @Nullable V value, @Nullable Throwable ex) {
    super(executor);
    this.value = value;
    this.executionException = ex;
  }

  @Override
  public boolean isFailed() {
    return executionException != null;
  }

  @Override
  @Nullable
  public V get() throws ExecutionException {
    if (this.executionException != null) {
      throw (this.executionException instanceof ExecutionException ?
              (ExecutionException) this.executionException :
              new ExecutionException(this.executionException));
    }
    return this.value;
  }

  @Override
  @Nullable
  public V get(long timeout, TimeUnit unit) throws ExecutionException {
    return get();
  }

  @Override
  public boolean isSuccess() {
    return executionException == null;
  }

  @Nullable
  @Override
  public Throwable getCause() {
    return exposedException(executionException);
  }

  @Nullable
  @Override
  public V getNow() {
    return value;
  }

  @Override
  public CompletableFuture<V> completable() {
    if (this.executionException != null) {
      CompletableFuture<V> completable = new CompletableFuture<>();
      completable.completeExceptionally(exposedException(this.executionException));
      return completable;
    }
    else {
      return CompletableFuture.completedFuture(this.value);
    }
  }

  @Override
  public CompleteFuture<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    Assert.notNull(listener, "listener is required");
    notifyListener(executor, this, listener);
    return this;
  }

  @Override
  public <C> CompleteFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    notifyListener(executor, this, FutureListener.forAdaption(listener, context));
    return this;
  }

  @Override
  public CompleteFuture<V> await() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return this;
  }

  @Override
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    await();
    return true;
  }

  @Override
  public boolean await(long timeoutMillis) throws InterruptedException {
    await();
    return true;
  }

  @Override
  public CompleteFuture<V> awaitUninterruptibly() {
    return this;
  }

  @Override
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    return true;
  }

  @Override
  public boolean awaitUninterruptibly(long timeoutMillis) {
    return true;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @param mayInterruptIfRunning this value has no effect in this implementation.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  /**
   * Determine the exposed exception: either the cause of a given
   * {@link ExecutionException}, or the original exception as-is.
   *
   * @return the exposed exception
   */
  @Nullable
  private static Throwable exposedException(@Nullable Throwable original) {
    if (original instanceof ExecutionException) {
      Throwable cause = original.getCause();
      if (cause != null) {
        return cause;
      }
    }
    return original;
  }

}
