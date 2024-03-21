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
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Default SettableFuture
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 17:27
 */
public class DefaultFuture<V> extends AbstractFuture<V> implements SettableFuture<V> {
  private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

  private static final Logger rejectedExecutionLogger =
          LoggerFactory.getLogger(DefaultFuture.class.getName() + ".rejectedExecution");

  @SuppressWarnings("rawtypes")
  private static final AtomicReferenceFieldUpdater<DefaultFuture, Object> RESULT_UPDATER =
          AtomicReferenceFieldUpdater.newUpdater(DefaultFuture.class, Object.class, "result");

  private static final Object SUCCESS = new Object();
  private static final Object UNCANCELLABLE = new Object();

  private static final CauseHolder CANCELLATION_CAUSE_HOLDER = new CauseHolder(
          StacklessCancellationException.newInstance(DefaultFuture.class, "cancel(...)"));

  private static final StackTraceElement[] CANCELLATION_STACK = CANCELLATION_CAUSE_HOLDER.cause.getStackTrace();

  @Nullable
  protected final Executor executor;

  @Nullable
  private volatile Object result;

  /**
   * One or more listeners. Can be a {@link FutureListener} or a {@link FutureListeners}.
   * If {@code null}, it means either 1) no listeners were added yet or 2) all listeners were notified.
   *
   * Threading - synchronized(this). We must support adding listeners when there is no Executor.
   */
  @Nullable
  private FutureListener<? extends Future<?>> listener;

  @Nullable
  private FutureListeners listeners;

  /**
   * Threading - synchronized(this). We are required to hold the monitor
   * to use Java's underlying wait()/notifyAll().
   */
  private short waiters;

  /**
   * Threading - synchronized(this). We must prevent concurrent
   * notification and FIFO listener notification if the executor changes.
   */
  private boolean notifyingListeners;

  /**
   * Creates a new instance.
   */
  public DefaultFuture() {
    this(null);
  }

  /**
   * Creates a new instance.
   *
   * @param executor the {@link Executor} which is used to notify
   * the SettableFuture once it is complete.
   */
  public DefaultFuture(@Nullable Executor executor) {
    this.executor = executor;
  }

  @Override
  public DefaultFuture<V> addListener(FutureListener<? extends Future<V>> listener) {
    Assert.notNull(listener, "listener is required");

    synchronized(this) {
      doAddListener(listener);
    }

    if (isDone()) {
      notifyListeners();
    }

    return this;
  }

  @Override
  public <C> DefaultFuture<V> addListener(FutureContextListener<C, ? extends Future<V>> listener, @Nullable C context) {
    return addListener(FutureListener.forAdaption(listener, context));
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
  public boolean setUncancellable() {
    if (RESULT_UPDATER.compareAndSet(this, null, UNCANCELLABLE)) {
      return true;
    }
    Object result = this.result;
    return !isDone(result) || !isCancelled(result);
  }

  @Override
  public boolean isSuccess() {
    Object result = this.result;
    return result != null && result != UNCANCELLABLE && !(result instanceof CauseHolder);
  }

  @Override
  public boolean isFailed() {
    return result instanceof CauseHolder;
  }

  @Override
  public boolean isCancellable() {
    return result == null;
  }

  @Override
  public boolean isCancelled() {
    return isCancelled(result);
  }

  @Override
  public boolean isDone() {
    return isDone(result);
  }

  @Override
  public Throwable getCause() {
    return getCause(result);
  }

  @Nullable
  private Throwable getCause(@Nullable Object result) {
    if (result instanceof CauseHolder) {
      if (result == CANCELLATION_CAUSE_HOLDER) {
        var ce = new LeanCancellationException();
        if (RESULT_UPDATER.compareAndSet(this, CANCELLATION_CAUSE_HOLDER, new CauseHolder(ce))) {
          return ce;
        }
        result = this.result;
      }
      if (result instanceof CauseHolder holder) {
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
    if (result instanceof CauseHolder || result == SUCCESS || result == UNCANCELLABLE) {
      return null;
    }
    return (V) result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get() throws InterruptedException, ExecutionException {
    Object result = this.result;
    if (!isDone(result)) {
      await();
      result = this.result;
    }
    if (result == SUCCESS || result == UNCANCELLABLE) {
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
    if (!isDone(result)) {
      if (!await(timeout, unit)) {
        throw new TimeoutException();
      }
      result = this.result;
    }
    if (result == SUCCESS || result == UNCANCELLABLE) {
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
  public Executor executor() {
    return executor;
  }

  /**
   * Subclasses can override this method to implement interruption of the future's
   * computation. The method is invoked automatically by a successful call to
   * {@link #cancel(boolean) cancel(true)}.
   * <p>The default implementation is empty.
   */
  protected void interruptTask() { }

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
    else if (result == UNCANCELLABLE) {
      buf.append("(uncancellable)");
    }
    else if (result instanceof CauseHolder) {
      buf.append("(failure: ")
              .append(((CauseHolder) result).cause)
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

  /**
   * Notify a listener that a future has completed.
   *
   * @param executor the executor to use to notify the listener {@code listener}.
   * @param future the future that is complete.
   * @param listener the listener to notify.
   */
  protected static void notifyListener(@Nullable Executor executor, final Future<?> future, final FutureListener<?> listener) {
    safeExecute(executor, () -> notifyListener(future, listener));
  }

  private void notifyListeners() {
    safeExecute(executor, this::notifyListenersNow);
  }

  private void notifyListenersNow() {
    FutureListener<?> listener;
    FutureListeners listeners;
    synchronized(this) {
      listener = this.listener;
      listeners = this.listeners;
      // Only proceed if there are listeners to notify and we are not already notifying listeners.
      if (notifyingListeners || (listener == null && listeners == null)) {
        return;
      }
      notifyingListeners = true;
      if (listener != null) {
        this.listener = null;
      }
      else {
        this.listeners = null;
      }
    }
    for (; ; ) {
      if (listener != null) {
        notifyListener(this, listener);
      }
      else {
        notifyListeners(listeners);
      }
      synchronized(this) {
        if (this.listener == null && this.listeners == null) {
          // Nothing can throw from within this method, so setting notifyingListeners back to false does not
          // need to be in a finally block.
          notifyingListeners = false;
          return;
        }
        listener = this.listener;
        listeners = this.listeners;
        if (listener != null) {
          this.listener = null;
        }
        else {
          this.listeners = null;
        }
      }
    }
  }

  private void notifyListeners(FutureListeners listeners) {
    final var a = listeners.listeners;
    final int size = listeners.size;
    for (int i = 0; i < size; i++) {
      notifyListener(this, a[i]);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void notifyListener(Future future, FutureListener l) {
    try {
      l.operationComplete(future);
    }
    catch (Throwable t) {
      logger.warn("An exception was thrown by {}.operationComplete(Future)", l.getClass().getName(), t);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  static void notifyListener(Future future, FutureContextListener l, @Nullable Object context) {
    try {
      l.operationComplete(future, context);
    }
    catch (Throwable t) {
      logger.warn("An exception was thrown by {}.operationComplete(Future, Context)", l.getClass().getName(), t);
    }
  }

  private void doAddListener(FutureListener<? extends Future<V>> listener) {
    if (this.listener == null) {
      if (listeners == null) {
        this.listener = listener;
      }
      else {
        listeners.add(listener);
      }
    }
    else {
      this.listeners = new FutureListeners(this.listener, listener);
      this.listener = null;
    }
  }

  private boolean doSetSuccess(@Nullable V result) {
    return doSetValue(result == null ? SUCCESS : result);
  }

  private boolean doSetFailure(Throwable cause) {
    Assert.notNull(cause, "Throwable cause is required");
    return doSetValue(new CauseHolder(cause));
  }

  private boolean doSetValue(Object objResult) {
    if (RESULT_UPDATER.compareAndSet(this, null, objResult)
            || RESULT_UPDATER.compareAndSet(this, UNCANCELLABLE, objResult)) {
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
    return listener != null || listeners != null;
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

  private void rethrowIfFailed() {
    Throwable cause = getCause();
    if (cause == null) {
      return;
    }

    throw ExceptionUtils.sneakyThrow(cause);
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

  /**
   * Notify all progressive listeners.
   * <p>
   * No attempt is made to ensure notification order if multiple
   * calls are made to this method before the original invocation completes.
   * <p>
   * This will do an iteration over all listeners to get all of type
   * {@link ProgressiveFutureListener}s.
   *
   * @param progress the new progress.
   * @param total the total progress.
   */
  @SuppressWarnings("unchecked")
  void notifyProgressiveListeners(final long progress, final long total) {
    final Object listeners = progressiveListeners();
    if (listeners == null) {
      return;
    }

    final ProgressiveFuture<V> self = (ProgressiveFuture<V>) this;

    if (listeners instanceof ProgressiveFutureListener<?>[] array) {
      safeExecute(executor, () -> notifyProgressiveListeners(self, array, progress, total));
    }
    else {
      final var l = (ProgressiveFutureListener<ProgressiveFuture<V>>) listeners;
      safeExecute(executor, () -> notifyProgressiveListener(self, l, progress, total));
    }
  }

  /**
   * Returns a {@link ProgressiveFutureListener}, an array of {@link ProgressiveFutureListener}, or
   * {@code null}.
   */
  @Nullable
  private synchronized Object progressiveListeners() {
    final FutureListener<?> listener = this.listener;
    final FutureListeners listeners = this.listeners;
    if (listener == null && listeners == null) {
      // No listeners added
      return null;
    }

    if (listeners != null) {
      return listeners.progressiveListeners;
    }
    else if (listener instanceof ProgressiveFutureListener) {
      return listener;
    }
    else {
      // Only one listener was added and it's not a progressive listener.
      return null;
    }
  }

  private static void notifyProgressiveListeners(ProgressiveFuture<?> future,
          ProgressiveFutureListener<?>[] listeners, long progress, long total) {
    for (var l : listeners) {
      notifyProgressiveListener(future, l, progress, total);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void notifyProgressiveListener(ProgressiveFuture future, ProgressiveFutureListener l, long progress, long total) {
    try {
      l.operationProgressed(future, progress, total);
    }
    catch (Throwable t) {
      logger.warn("An exception was thrown by {}.operationProgressed()", l.getClass().getName(), t);
    }
  }

  private static boolean isCancelled(@Nullable Object result) {
    return result instanceof CauseHolder
            && ((CauseHolder) result).cause instanceof CancellationException;
  }

  private static boolean isDone(@Nullable Object result) {
    return result != null && result != UNCANCELLABLE;
  }

  private static final class CauseHolder {
    public final Throwable cause;

    CauseHolder(Throwable cause) {
      this.cause = cause;
    }
  }

  static void safeExecute(@Nullable Executor executor, Runnable task) {
    if (executor == null) {
      executor = defaultExecutor;
    }
    try {
      executor.execute(task);
    }
    catch (Throwable t) {
      rejectedExecutionLogger.error(
              "Failed to submit a listener notification task. Executor shutting-down?", t);
    }
  }

  private static final class LeanCancellationException extends CancellationException {
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

  private static final class StacklessCancellationException extends CancellationException {

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
