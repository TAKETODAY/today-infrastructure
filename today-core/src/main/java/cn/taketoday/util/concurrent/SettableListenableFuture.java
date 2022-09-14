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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A {@link ListenableFuture} whose value can be set via {@link #set(Object)}
 * or {@link #setException(Throwable)}. It may also get cancelled.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.SettableFuture}.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Mattias Severson
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

  private static final Callable<Object> DUMMY_CALLABLE = () -> {
    throw new IllegalStateException("Should never be called");
  };

  private final SettableTask<T> settableTask = new SettableTask<>();

  /**
   * Set the value of this future. This method will return {@code true} if the
   * value was set successfully, or {@code false} if the future has already been
   * set or cancelled.
   *
   * @param value the value that will be set
   * @return {@code true} if the value was successfully set, else {@code false}
   */
  public boolean set(@Nullable T value) {
    return this.settableTask.setResultValue(value);
  }

  /**
   * Set the exception of this future. This method will return {@code true} if the
   * exception was set successfully, or {@code false} if the future has already been
   * set or cancelled.
   *
   * @param exception the value that will be set
   * @return {@code true} if the exception was successfully set, else {@code false}
   */
  public boolean setException(Throwable exception) {
    Assert.notNull(exception, "Exception must not be null");
    return this.settableTask.setExceptionResult(exception);
  }

  @Override
  public void addCallback(ListenableFutureCallback<? super T> callback) {
    this.settableTask.addCallback(callback);
  }

  @Override
  public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
    this.settableTask.addCallback(successCallback, failureCallback);
  }

  @Override
  public CompletableFuture<T> completable() {
    return this.settableTask.completable();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
    if (cancelled && mayInterruptIfRunning) {
      interruptTask();
    }
    return cancelled;
  }

  @Override
  public boolean isCancelled() {
    return this.settableTask.isCancelled();
  }

  @Override
  public boolean isDone() {
    return this.settableTask.isDone();
  }

  /**
   * Retrieve the value.
   * <p>This method returns the value if it has been set via {@link #set(Object)},
   * throws an {@link ExecutionException} if an exception has
   * been set via {@link #setException(Throwable)}, or throws a
   * {@link java.util.concurrent.CancellationException} if the future has been cancelled.
   *
   * @return the value associated with this future
   */
  @Override
  @Nullable
  public T get() throws InterruptedException, ExecutionException {
    return this.settableTask.get();
  }

  /**
   * Retrieve the value.
   * <p>This method returns the value if it has been set via {@link #set(Object)},
   * throws an {@link ExecutionException} if an exception has
   * been set via {@link #setException(Throwable)}, or throws a
   * {@link java.util.concurrent.CancellationException} if the future has been cancelled.
   *
   * @param timeout the maximum time to wait
   * @param unit the unit of the timeout argument
   * @return the value associated with this future
   */
  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return this.settableTask.get(timeout, unit);
  }

  /**
   * Subclasses can override this method to implement interruption of the future's
   * computation. The method is invoked automatically by a successful call to
   * {@link #cancel(boolean) cancel(true)}.
   * <p>The default implementation is empty.
   */
  protected void interruptTask() {
  }

  private static class SettableTask<T> extends ListenableFutureTask<T> {

    @Nullable
    private volatile Thread completingThread;

    @SuppressWarnings("unchecked")
    public SettableTask() {
      super((Callable<T>) DUMMY_CALLABLE);
    }

    public boolean setResultValue(@Nullable T value) {
      set(value);
      return checkCompletingThread();
    }

    public boolean setExceptionResult(Throwable exception) {
      setException(exception);
      return checkCompletingThread();
    }

    @Override
    protected void done() {
      if (!isCancelled()) {
        // Implicitly invoked by set/setException: store current thread for
        // determining whether the given result has actually triggered completion
        // (since FutureTask.set/setException unfortunately don't expose that)
        this.completingThread = Thread.currentThread();
      }
      super.done();
    }

    private boolean checkCompletingThread() {
      boolean check = (this.completingThread == Thread.currentThread());
      if (check) {
        this.completingThread = null;  // only first match actually counts
      }
      return check;
    }
  }

}
