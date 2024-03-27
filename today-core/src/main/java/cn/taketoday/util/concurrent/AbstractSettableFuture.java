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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import cn.taketoday.lang.Nullable;

/**
 * Abstract {@link Future} implementation which allow for cancellation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/26 22:10
 */
public abstract class AbstractSettableFuture<V> extends AbstractFuture<V> {

  /**
   * The run state of this {@code Future}, initially {@link #NEW}. The run state
   * transitions to a terminal state only in methods {@link #trySuccess(Object)},
   * {@link #tryFailure(Throwable)}, and cancel. During completion, state may
   * take on transient values of {@link #COMPLETING} (while outcome is being set)
   * or {@link #INTERRUPTING} (only while interrupting the runner to satisfy a
   * cancel(true)). Transitions from these intermediate to final states use
   * cheaper ordered/lazy writes because values are unique and cannot be further
   * modified.
   *
   * <pre>
   * Possible state transitions:
   * NEW -> COMPLETING -> NORMAL
   * NEW -> COMPLETING -> EXCEPTIONAL
   * NEW -> CANCELLED
   * NEW -> INTERRUPTING -> INTERRUPTED
   * </pre>
   */
  protected volatile int state;

  protected static final int NEW = 0;
  protected static final int COMPLETING = 1;
  protected static final int NORMAL = 2;
  protected static final int EXCEPTIONAL = 3;
  protected static final int CANCELLED = 4;
  protected static final int INTERRUPTING = 5;
  protected static final int INTERRUPTED = 6;

  /** Treiber stack of waiting threads */
  @Nullable
  private volatile Waiter waiters;

  /** The result to return or exception to throw from get() */
  @Nullable
  private Object result; // non-volatile, protected by state reads/writes

  /**
   * @param executor The {@link Executor} which is used to notify
   * the {@code Future} once it is complete.
   */
  protected AbstractSettableFuture(@Nullable Executor executor) {
    super(executor);
    this.state = NEW;
  }

  @Override
  public boolean isSuccess() {
    return state == NORMAL;
  }

  @Override
  public boolean isFailed() {
    return state > NORMAL;
  }

  @Override
  public boolean isCancelled() {
    return state >= CANCELLED;
  }

  @Override
  public boolean isDone() {
    return state != NEW;
  }

  @Override
  public boolean cancel() {
    return cancel(true);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (!(state == NEW && STATE.compareAndSet(this, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) {
      return false;
    }
    try {
      // in case call to interrupt throws exception
      if (mayInterruptIfRunning) {
        interruptTask();
      }
    }
    finally {
      finishCompletion();
    }
    return true;
  }

  /**
   * Subclasses can override this method to implement interruption of the future's
   * computation. The method is invoked automatically by a successful call to
   * {@link #cancel(boolean) cancel(true)}.
   * <p>The default implementation is empty.
   */
  protected void interruptTask() {
    STATE.setRelease(this, INTERRUPTED);
  }

  @Override
  public Throwable getCause() {
    int s = state;
    if (s == EXCEPTIONAL) {
      return (Throwable) result;
    }
    else if (s >= CANCELLED) {
      return new LeanCancellationException();
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V getNow() {
    return state == NORMAL ? (V) result : null;
  }

  /**
   * @throws CancellationException {@inheritDoc}
   */
  @Nullable
  public V get() throws InterruptedException, ExecutionException {
    int s = state;
    if (s <= COMPLETING)
      s = awaitDone(false, 0L);
    return report(s);
  }

  /**
   * @throws CancellationException {@inheritDoc}
   */
  @Nullable
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    int s = state;
    if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) {
      throw new TimeoutException();
    }
    return report(s);
  }

  @Override
  public AbstractSettableFuture<V> sync() throws InterruptedException {
    await();
    rethrowIfFailed();
    return this;
  }

  @Override
  public AbstractSettableFuture<V> syncUninterruptibly() {
    awaitUninterruptibly();
    rethrowIfFailed();
    return this;
  }

  @Override
  public AbstractSettableFuture<V> await() throws InterruptedException {
    if (state <= COMPLETING) {
      awaitDone(false, 0L);
    }
    return this;
  }

  @Override
  public AbstractSettableFuture<V> awaitUninterruptibly() {
    if (state <= COMPLETING) {
      try {
        awaitDone(false, 0L);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return this;
  }

  @Override
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    return state <= COMPLETING
            && awaitDone(true, unit.toNanos(timeout)) <= COMPLETING;
  }

  @Override
  public boolean await(long timeoutMillis) throws InterruptedException {
    return state <= COMPLETING
            && awaitDone(true, TimeUnit.MILLISECONDS.toNanos(timeoutMillis)) <= COMPLETING;
  }

  /**
   * Protected method invoked when this task transitions to state
   * {@code isDone} (whether normally or via cancellation). The
   * default implementation does nothing.  Subclasses may override
   * this method to invoke completion callbacks or perform
   * bookkeeping. Note that you can query status inside the
   * implementation of this method to determine whether this task
   * has been cancelled.
   */
  protected void done() {
    notifyListeners();
  }

  /**
   * Sets the result of this future to the given value unless
   * this future has already been set or has been cancelled.
   *
   * <p>This method is invoked internally by the {@link #run} method
   * upon successful completion of the computation.
   *
   * @param v the value
   * @return {@code true} if and only if successfully marked this future as
   * a success. Otherwise {@code false} because this future is
   * already marked as either a success or a failure.
   */
  public boolean trySuccess(@Nullable V v) {
    if (STATE.compareAndSet(this, NEW, COMPLETING)) {
      result = v;
      STATE.setRelease(this, NORMAL); // final state
      finishCompletion();
      return true;
    }
    return false;
  }

  /**
   * Causes this future to report an {@link ExecutionException}
   * with the given throwable as its cause, unless this future has
   * already been set or has been cancelled.
   *
   * <p>This method is invoked internally by the {@link #run} method
   * upon failure of the computation.
   *
   * @param t the cause of failure
   * @return {@code true} if and only if successfully marked this future as
   * a failure. Otherwise {@code false} because this future is
   * already marked as either a success or a failure.
   */
  public boolean tryFailure(Throwable t) {
    if (STATE.compareAndSet(this, NEW, COMPLETING)) {
      // Assert.notNull(cause, "Throwable cause is required");
      result = t;
      STATE.setRelease(this, EXCEPTIONAL); // final state
      finishCompletion();
      return true;
    }
    return false;
  }

  /**
   * Returns result or throws exception for completed task.
   *
   * @param s completed state value
   */
  @SuppressWarnings("unchecked")
  private V report(int s) throws ExecutionException {
    Object x = result;
    if (s == NORMAL) {
      return (V) x;
    }
    if (s >= CANCELLED) {
      throw new CancellationException();
    }
    throw new ExecutionException((Throwable) x);
  }

  /**
   * Simple linked list nodes to record waiting threads in a Treiber
   * stack.  See other classes such as Phaser and SynchronousQueue
   * for more detailed explanation.
   */
  static final class Waiter {

    @Nullable
    volatile Thread thread = Thread.currentThread();

    @Nullable
    volatile Waiter next;

  }

  /**
   * Removes and signals all waiting threads, invokes done(), and
   * nulls out callable.
   */
  private void finishCompletion() {
    // assert state > COMPLETING;
    for (Waiter q; (q = waiters) != null; ) {
      if (WAITERS.weakCompareAndSet(this, q, null)) {
        while (true) {
          Thread t = q.thread;
          if (t != null) {
            q.thread = null;
            LockSupport.unpark(t);
          }
          Waiter next = q.next;
          if (next == null)
            break;
          q.next = null; // unlink to help gc
          q = next;
        }
        break;
      }
    }
    done();
  }

  /**
   * Awaits completion or aborts on interrupt or timeout.
   *
   * @param timed true if use timed waits
   * @param nanos time to wait, if timed
   * @return state upon completion or at timeout
   */
  private int awaitDone(boolean timed, long nanos) throws InterruptedException {
    // The code below is very delicate, to achieve these goals:
    // - call nanoTime exactly once for each call to park
    // - if nanos <= 0L, return promptly without allocation or nanoTime
    // - if nanos == Long.MIN_VALUE, don't underflow
    // - if nanos == Long.MAX_VALUE, and nanoTime is non-monotonic
    //   and we suffer a spurious wakeup, we will do no worse than
    //   to park-spin for a while
    long startTime = 0L;    // Special value 0L means not yet parked
    Waiter q = null;
    boolean queued = false;
    while (true) {
      int s = state;
      if (s > COMPLETING) {
        if (q != null) {
          q.thread = null;
        }
        return s;
      }
      else if (s == COMPLETING) {
        // We may have already promised (via isDone) that we are done
        // so never return empty-handed or throw InterruptedException
        Thread.yield();
      }
      else if (Thread.interrupted()) {
        removeWaiter(q);
        throw new InterruptedException();
      }
      else if (q == null) {
        if (timed && nanos <= 0L) {
          return s;
        }
        q = new Waiter();
      }
      else if (!queued) {
        queued = WAITERS.weakCompareAndSet(this, q.next = waiters, q);
      }
      else if (timed) {
        final long parkNanos;
        if (startTime == 0L) { // first time
          startTime = System.nanoTime();
          if (startTime == 0L) {
            startTime = 1L;
          }
          parkNanos = nanos;
        }
        else {
          long elapsed = System.nanoTime() - startTime;
          if (elapsed >= nanos) {
            removeWaiter(q);
            return state;
          }
          parkNanos = nanos - elapsed;
        }
        // nanoTime may be slow; recheck before parking
        if (state < COMPLETING) {
          LockSupport.parkNanos(this, parkNanos);
        }
      }
      else {
        LockSupport.park(this);
      }
    }
  }

  /**
   * Tries to unlink a timed-out or interrupted wait node to avoid
   * accumulating garbage.  Internal nodes are simply unspliced
   * without CAS since it is harmless if they are traversed anyway
   * by releasers.  To avoid effects of unsplicing from already
   * removed nodes, the list is retraversed in case of an apparent
   * race.  This is slow when there are a lot of nodes, but we don't
   * expect lists to be long enough to outweigh higher-overhead
   * schemes.
   */
  private void removeWaiter(@Nullable Waiter node) {
    if (node != null) {
      node.thread = null;
      retry:
      while (true) {        // restart on removeWaiter race
        for (Waiter pred = null, q = waiters, s; q != null; q = s) {
          s = q.next;
          if (q.thread != null) {
            pred = q;
          }
          else if (pred != null) {
            pred.next = s;
            if (pred.thread == null) { // check for race
              continue retry;
            }
          }
          else if (!WAITERS.compareAndSet(this, q, s)) {
            continue retry;
          }
        }
        break;
      }
    }
  }

  /**
   * Returns a string representation of this FutureTask.
   * <p>
   * The default implementation returns a string identifying this
   * FutureTask, as well as its completion state.  The state, in
   * brackets, contains one of the strings {@code "Completed Normally"},
   * {@code "Completed Exceptionally"}, {@code "Cancelled"}, or {@code
   * "Not completed"}.
   *
   * @return a string representation of this FutureTask
   */
  @Override
  public String toString() {
    final String status = switch (state) {
      case NORMAL -> "[Completed normally]";
      case EXCEPTIONAL -> "[Completed exceptionally: %s]".formatted(result);
      case CANCELLED, INTERRUPTING, INTERRUPTED -> "[Cancelled]";
      default -> notCompletedString();
    };
    return super.toString() + status;
  }

  protected String notCompletedString() {
    return "[Not completed]";
  }

  // VarHandle mechanics
  private static final VarHandle STATE;
  private static final VarHandle WAITERS;

  static {
    try {
      var l = MethodHandles.lookup();
      STATE = l.findVarHandle(AbstractSettableFuture.class, "state", int.class);
      WAITERS = l.findVarHandle(AbstractSettableFuture.class, "waiters", Waiter.class);
    }
    catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static final class LeanCancellationException extends CancellationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final StackTraceElement[] CANCELLATION_STACK = {
            new StackTraceElement(AbstractSettableFuture.class.getName(), "cancel(...)", null, -1)
    };

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

}
