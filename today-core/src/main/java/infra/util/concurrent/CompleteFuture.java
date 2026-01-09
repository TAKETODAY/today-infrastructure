/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import infra.lang.Assert;

/**
 * A skeletal {@link Future} implementation which represents
 * a {@link Future} which has been completed already.
 *
 * @param <V> value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 21:28
 */
final class CompleteFuture<V extends @Nullable Object> extends Future<V> {

  private final @Nullable V value;

  private final @Nullable Throwable executionException;

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
  public boolean isFailure() {
    return executionException != null;
  }

  @Override
  public V get() throws ExecutionException {
    if (this.executionException != null) {
      throw (this.executionException instanceof ExecutionException ?
              (ExecutionException) this.executionException :
              new ExecutionException(this.executionException));
    }
    return this.value;
  }

  @Override
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
  public boolean cancel(@Nullable Throwable cancellation, boolean mayInterruptIfRunning) {
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
