/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link FutureTask} that implements {@link ListenableFuture}.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Arjen Poutsma
 * @since 4.0
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

  private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<>();

  /**
   * Create a new {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Callable}.
   *
   * @param callable the callable task
   */
  public ListenableFutureTask(Callable<T> callable) {
    super(callable);
  }

  /**
   * Create a {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Runnable}, and arrange that {@link #get()}
   * will return the given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion
   */
  public ListenableFutureTask(Runnable runnable, @Nullable T result) {
    super(runnable, result);
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
    CompletableFuture<T> completable = new DelegatingCompletableFuture<>(this);
    this.callbacks.addSuccessCallback(completable::complete);
    this.callbacks.addFailureCallback(completable::completeExceptionally);
    return completable;
  }

  @Override
  protected void done() {
    Throwable cause;
    try {
      T result = get();
      this.callbacks.success(result);
      return;
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return;
    }
    catch (ExecutionException ex) {
      cause = ex.getCause();
      if (cause == null) {
        cause = ex;
      }
    }
    catch (Throwable ex) {
      cause = ex;
    }
    this.callbacks.failure(cause);
  }

}
