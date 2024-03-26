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

import java.io.Serial;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Default SettableFuture
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 17:27
 */
public class DefaultFuture<V> extends AbstractFuture<V> implements SettableFuture<V> {

  @SuppressWarnings("rawtypes")
  private static final AtomicReferenceFieldUpdater<DefaultFuture, Object> RESULT_UPDATER =
          AtomicReferenceFieldUpdater.newUpdater(DefaultFuture.class, Object.class, "result");

  private static final Object SUCCESS = new Object();

  private static final Failure CANCELLATION_CAUSE_HOLDER = new Failure(
          StacklessCancellationException.newInstance(DefaultFuture.class, "cancel(...)"));

  private static final StackTraceElement[] CANCELLATION_STACK = CANCELLATION_CAUSE_HOLDER.cause.getStackTrace();

  @Nullable
  private volatile Object result;

  /**
   * Threading - synchronized(this). We are required to hold the monitor
   * to use Java's underlying wait()/notifyAll().
   */
  private short waiters;

  /**
   * Creates a new instance.
   *
   * @see #defaultExecutor
   */
  DefaultFuture() {
    super(defaultExecutor);
  }

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the SettableFuture once it is complete.
   */
  DefaultFuture(@Nullable Executor executor) {
    super(executor);
  }

  @Override
  public DefaultFuture<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    super.onCompleted(listener);
    return this;
  }

  @Override
  public <C> DefaultFuture<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    return onCompleted(FutureListener.forAdaption(listener, context));
  }

  @Override
  public DefaultFuture<V> setSuccess(@Nullable V result) {
    if (doSetSuccess(result)) {
      return this;
    }
    throw new IllegalStateException("complete already: " + this);
  }

  @Override
  public boolean trySuccess(@Nullable V result) {
    return doSetSuccess(result);
  }

  @Override
  public SettableFuture<V> setFailure(Throwable cause) {
    if (doSetFailure(cause)) {
      return this;
    }
    throw new IllegalStateException("complete already: " + this, cause);
  }

  @Override
  public boolean tryFailure(Throwable cause) {
    return doSetFailure(cause);
  }

  @Override
  public boolean isSuccess() {
    Object result = this.result;
    return result != null && !(result instanceof Failure);
  }

  @Override
  public boolean isFailed() {
    return result instanceof Failure;
  }

  @Override
  public boolean isCancelled() {
    return isCancelled(result);
  }

  @Override
  public boolean isDone() {
    return result != null;
  }

  @Override
  public Throwable getCause() {
    return getCause(result);
  }

  @Nullable
  private Throwable getCause(@Nullable Object result) {
    if (result instanceof Failure) {
      if (result == CANCELLATION_CAUSE_HOLDER) {
        var ce = new LeanCancellationException();
        if (RESULT_UPDATER.compareAndSet(this, CANCELLATION_CAUSE_HOLDER, new Failure(ce))) {
          return ce;
        }
        result = this.result;
      }
      if (result instanceof Failure holder) {
        return holder.cause;
      }
    }
    return null;
  }

  @Override
  public SettableFuture<V> sync() throws InterruptedException {
    await();
    rethrowIfFailed();
    return this;
  }

  @Override
  public SettableFuture<V> syncUninterruptibly() {
    awaitUninterruptibly();
    rethrowIfFailed();
    return this;
  }

  @Override
  public SettableFuture<V> await() throws InterruptedException {
    if (isDone()) {
      return this;
    }

    if (Thread.interrupted()) {
      throw new InterruptedException(toString());
    }

    synchronized(this) {
      while (!isDone()) {
        incWaiters();
        try {
          wait();
        }
        finally {
          decWaiters();
        }
      }
    }
    return this;
  }

  @Override
  public SettableFuture<V> awaitUninterruptibly() {
    if (isDone()) {
      return this;
    }

    boolean interrupted = false;
    synchronized(this) {
      while (!isDone()) {
        incWaiters();
        try {
          wait();
        }
        catch (InterruptedException e) {
          // Interrupted while waiting.
          interrupted = true;
        }
        finally {
          decWaiters();
        }
      }
    }

    if (interrupted) {
      Thread.currentThread().interrupt();
    }

    return this;
  }

  @Override
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    return doAwait(unit.toNanos(timeout), true);
  }

  @Override
  public boolean await(long timeoutMillis) throws InterruptedException {
    return doAwait(MILLISECONDS.toNanos(timeoutMillis), true);
  }

  @Override
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    try {
      return doAwait(unit.toNanos(timeout), false);
    }
    catch (InterruptedException e) {
      // Should not be raised at all.
      throw new InternalError();
    }
  }

  @Override
  public boolean awaitUninterruptibly(long timeoutMillis) {
    try {
      return doAwait(MILLISECONDS.toNanos(timeoutMillis), false);
    }
    catch (InterruptedException e) {
      // Should not be raised at all.
      throw new InternalError();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public V getNow() {
    Object result = this.result;
    if (result instanceof Failure || result == SUCCESS) {
      return null;
    }
    return (V) result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get() throws InterruptedException, ExecutionException {
    Object result = this.result;
    if (result == null) {
      await();
      result = this.result;
    }
    if (result == SUCCESS) {
      return null;
    }
    Throwable cause = getCause(result);
    if (cause == null) {
      return (V) result;
    }
    if (cause instanceof CancellationException) {
      throw (CancellationException) cause;
    }
    throw new ExecutionException(cause);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    Object result = this.result;
    if (result == null) {
      if (!await(timeout, unit)) {
        throw new TimeoutException();
      }
      result = this.result;
    }
    if (result == SUCCESS) {
      return null;
    }
    Throwable cause = getCause(result);
    if (cause == null) {
      return (V) result;
    }
    if (cause instanceof CancellationException) {
      throw (CancellationException) cause;
    }
    throw new ExecutionException(cause);
  }

  @Override
  public boolean cancel() {
    return cancel(true);
  }

  /**
   * {@inheritDoc}
   *
   * @param mayInterruptIfRunning this value has no effect in this implementation.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (RESULT_UPDATER.compareAndSet(this, null, CANCELLATION_CAUSE_HOLDER)) {
      if (checkNotifyWaiters()) {
        notifyListeners();
      }
      if (mayInterruptIfRunning) {
        interruptTask();
      }
      return true;
    }
    return false;
  }

  @Override
  public DefaultFuture<V> cascadeTo(final SettableFuture<V> settable) {
    Futures.cascade(this, settable);
    return this;
  }

  /**
   * Subclasses can override this method to implement interruption of the future's
   * computation. The method is invoked automatically by a successful call to
   * {@link #cancel(boolean) cancel(true)}.
   * <p>The default implementation is empty.
   */
  protected void interruptTask() {
    // noop
  }

  @Override
  public String toString() {
    return toStringBuilder().toString();
  }

  protected StringBuilder toStringBuilder() {
    StringBuilder buf = new StringBuilder(64)
            .append(ClassUtils.getSimpleName(getClass().getName()))
            .append('@')
            .append(Integer.toHexString(hashCode()));

    Object result = this.result;
    if (result == SUCCESS) {
      buf.append("(success)");
    }
    else if (result instanceof Failure) {
      buf.append("(failure: ")
              .append(((Failure) result).cause)
              .append(')');
    }
    else if (result != null) {
      buf.append("(success: ")
              .append(result)
              .append(')');
    }
    else {
      buf.append("(incomplete)");
    }

    return buf;
  }

  private boolean doSetSuccess(@Nullable V result) {
    return doSetValue(result == null ? SUCCESS : result);
  }

  private boolean doSetFailure(Throwable cause) {
    Assert.notNull(cause, "Throwable cause is required");
    return doSetValue(new Failure(cause));
  }

  private boolean doSetValue(Object objResult) {
    if (RESULT_UPDATER.compareAndSet(this, null, objResult)) {
      if (checkNotifyWaiters()) {
        notifyListeners();
      }
      return true;
    }
    return false;
  }

  /**
   * Check if there are any waiters and if so notify these.
   *
   * @return {@code true} if there are any listeners attached to the SettableFuture, {@code false} otherwise.
   */
  private synchronized boolean checkNotifyWaiters() {
    if (waiters > 0) {
      notifyAll();
    }
    return listeners != null;
  }

  private void incWaiters() {
    if (waiters == Short.MAX_VALUE) {
      throw new IllegalStateException("too many waiters: " + this);
    }
    ++waiters;
  }

  private void decWaiters() {
    --waiters;
  }

  private boolean doAwait(long timeoutNanos, boolean interruptable) throws InterruptedException {
    if (isDone()) {
      return true;
    }

    if (timeoutNanos <= 0) {
      return isDone();
    }

    if (interruptable && Thread.interrupted()) {
      throw new InterruptedException(toString());
    }

    // Start counting time from here instead of the first line of this method,
    // to avoid/postpone performance cost of System.nanoTime().
    final long startTime = System.nanoTime();
    synchronized(this) {
      boolean interrupted = false;
      try {
        long waitTime = timeoutNanos;
        while (!isDone() && waitTime > 0) {
          incWaiters();
          try {
            wait(waitTime / 1000000, (int) (waitTime % 1000000));
          }
          catch (InterruptedException e) {
            if (interruptable) {
              throw e;
            }
            else {
              interrupted = true;
            }
          }
          finally {
            decWaiters();
          }
          // Check isDone() in advance, try to avoid calculating the elapsed time later.
          if (isDone()) {
            return true;
          }
          // Calculate the elapsed time here instead of in the while condition,
          // try to avoid performance cost of System.nanoTime() in the first loop of while.
          waitTime = timeoutNanos - (System.nanoTime() - startTime);
        }
        return isDone();
      }
      finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private static boolean isCancelled(@Nullable Object result) {
    return result instanceof Failure && ((Failure) result).cause instanceof CancellationException;
  }

  private static final class Failure {
    public final Throwable cause;

    Failure(Throwable cause) {
      this.cause = cause;
    }
  }

  static final class LeanCancellationException extends CancellationException {

    @Serial
    private static final long serialVersionUID = 1L;

    // Suppress a warning since the method doesn't need synchronization
    @Override
    public Throwable fillInStackTrace() {
      setStackTrace(CANCELLATION_STACK);
      return this;
    }

    @Override
    public String toString() {
      return CancellationException.class.getName();
    }
  }

  static final class StacklessCancellationException extends CancellationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private StacklessCancellationException() { }

    // Override fillInStackTrace() so we not populate the backtrace via a native call and so leak the
    // Classloader.
    @Override
    public Throwable fillInStackTrace() {
      return this;
    }

    static StacklessCancellationException newInstance(Class<?> clazz, String method) {
      return unknownStackTrace(new StacklessCancellationException(), clazz, method);
    }

    static <T extends Throwable> T unknownStackTrace(T cause, Class<?> clazz, String method) {
      cause.setStackTrace(new StackTraceElement[] { new StackTraceElement(clazz.getName(), method, null, -1) });
      return cause;
    }
  }

}
