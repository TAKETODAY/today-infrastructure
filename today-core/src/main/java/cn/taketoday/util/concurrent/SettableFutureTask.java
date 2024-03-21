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
import java.util.concurrent.Executor;
import java.util.concurrent.RunnableFuture;

import cn.taketoday.lang.Nullable;

class SettableFutureTask<V> extends DefaultFuture<V> implements RunnableFuture<V> {

  private static final Runnable COMPLETED = new SentinelRunnable("COMPLETED");

  private static final Runnable CANCELLED = new SentinelRunnable("CANCELLED");

  private static final Runnable FAILED = new SentinelRunnable("FAILED");

  // Strictly of type Callable<V> or Runnable
  private Object task;

  SettableFutureTask(@Nullable Executor executor, Runnable runnable, @Nullable V result) {
    super(executor);
    task = result == null ? runnable : new RunnableAdapter<>(runnable, result);
  }

  SettableFutureTask(@Nullable Executor executor, Callable<V> callable) {
    super(executor);
    task = callable;
  }

  @Override
  public final int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public final boolean equals(Object obj) {
    return this == obj;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  V runTask() throws Throwable {
    final Object task = this.task;
    if (task instanceof Callable) {
      return ((Callable<V>) task).call();
    }
    ((Runnable) task).run();
    return null;
  }

  @Override
  public void run() {
    try {
      if (setUncancellableInternal()) {
        V result = runTask();
        setSuccessInternal(result);
      }
    }
    catch (Throwable e) {
      setFailureInternal(e);
    }
  }

  private boolean clearTaskAfterCompletion(boolean done, Runnable result) {
    if (done) {
      // The only time where it might be possible for the sentinel task
      // to be called is in the case of a periodic ScheduledFutureTask,
      // in which case it's a benign race with cancellation and the (null)
      // return value is not used.
      task = result;
    }
    return done;
  }

  @Override
  public final SettableFutureTask<V> setFailure(Throwable cause) {
    throw new IllegalStateException();
  }

  protected final void setFailureInternal(Throwable cause) {
    super.setFailure(cause);
    clearTaskAfterCompletion(true, FAILED);
  }

  @Override
  public final boolean tryFailure(Throwable cause) {
    return false;
  }

  @Override
  public final SettableFutureTask<V> setSuccess(@Nullable V result) {
    throw new IllegalStateException();
  }

  protected final SettableFutureTask<V> setSuccessInternal(@Nullable V result) {
    super.setSuccess(result);
    clearTaskAfterCompletion(true, COMPLETED);
    return this;
  }

  @Override
  public final boolean trySuccess(@Nullable V result) {
    return false;
  }

  @Override
  public final boolean setUncancellable() {
    throw new IllegalStateException();
  }

  protected final boolean setUncancellableInternal() {
    return super.setUncancellable();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return clearTaskAfterCompletion(super.cancel(mayInterruptIfRunning), CANCELLED);
  }

  @Override
  protected StringBuilder toStringBuilder() {
    StringBuilder buf = super.toStringBuilder();
    buf.setCharAt(buf.length() - 1, ',');

    return buf.append(" task: ")
            .append(task)
            .append(')');
  }

  private static class SentinelRunnable implements Runnable {
    private final String name;

    SentinelRunnable(String name) {
      this.name = name;
    }

    @Override
    public void run() { } // no-op

    @Override
    public String toString() {
      return name;
    }
  }

  private static final class RunnableAdapter<T> implements Callable<T> {
    final Runnable task;
    final T result;

    RunnableAdapter(Runnable task, T result) {
      this.task = task;
      this.result = result;
    }

    @Override
    public T call() {
      task.run();
      return result;
    }

    @Override
    public String toString() {
      return "Callable(task: %s, result: %s)".formatted(task, result);
    }

  }

}
