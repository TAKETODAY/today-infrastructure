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

package cn.taketoday.scheduling.annotation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.concurrent.CompleteFuture;
import cn.taketoday.util.concurrent.ListenableFuture;

/**
 * A pass-through {@code Future} handle that can be used for method signatures
 * which are declared with a {@code Future} return type for asynchronous execution.
 *
 * <p>this class implements {@link ListenableFuture}, not just
 * plain {@link Future}, along with the corresponding support
 * in {@code @Async} processing.
 *
 * <p>this class also supports passing execution exceptions back
 * to the caller.
 *
 * @param <V> the value type
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Async
 * @see #forValue(Object)
 * @see #forExecutionException(Throwable)
 * @since 4.0
 */
public class AsyncResult<V> extends CompleteFuture<V> implements ListenableFuture<V> {

  @Nullable
  private final V value;

  @Nullable
  private final Throwable executionException;

  /**
   * Create a new AsyncResult holder.
   *
   * @param value the value to pass through
   */
  public AsyncResult(@Nullable V value) {
    this(value, null);
  }

  /**
   * Create a new AsyncResult holder.
   *
   * @param value the value to pass through
   */
  private AsyncResult(@Nullable V value, @Nullable Throwable ex) {
    this.value = value;
    this.executionException = ex;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
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
  public Throwable cause() {
    return executionException;
  }

  @Nullable
  @Override
  public V getNow() {
    return value;
  }

  @Override
  public ListenableFuture<V> sync() {
    if (executionException != null) {
      throw ExceptionUtils.sneakyThrow(executionException);
    }
    return this;
  }

  @Override
  public ListenableFuture<V> syncUninterruptibly() {
    if (executionException != null) {
      throw ExceptionUtils.sneakyThrow(executionException);
    }
    return this;
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

  /**
   * Create a new async result which exposes the given value from {@link Future#get()}.
   *
   * @param value the value to expose
   * @see Future#get()
   */
  public static <V> ListenableFuture<V> forValue(V value) {
    return new AsyncResult<>(value, null);
  }

  /**
   * Create a new async result which exposes the given exception as an
   * {@link ExecutionException} from {@link Future#get()}.
   *
   * @param ex the exception to expose (either an pre-built {@link ExecutionException}
   * or a cause to be wrapped in an {@link ExecutionException})
   * @see ExecutionException
   */
  public static <V> ListenableFuture<V> forExecutionException(Throwable ex) {
    return new AsyncResult<>(null, ex);
  }

  /**
   * Determine the exposed exception: either the cause of a given
   * {@link ExecutionException}, or the original exception as-is.
   *
   * @param original the original as given to {@link #forExecutionException}
   * @return the exposed exception
   */
  private static Throwable exposedException(Throwable original) {
    if (original instanceof ExecutionException) {
      Throwable cause = original.getCause();
      if (cause != null) {
        return cause;
      }
    }
    return original;
  }

}
