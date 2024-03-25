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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import cn.taketoday.lang.Nullable;

/**
 * Listenable {@link FutureTask}
 *
 * @param <V> the result type returned by this Future's {@code get} method
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FutureTask
 * @since 4.0
 */
public class ListenableFutureTask<V> extends AbstractFuture<V> implements RunnableFuture<V> {

  /*
   * Revision notes: This differs from previous versions of this
   * class that relied on AbstractQueuedSynchronizer, mainly to
   * avoid surprising users about retaining interrupt status during
   * cancellation races. Sync control in the current design relies
   * on a "state" field updated via CAS to track completion, along
   * with a simple Treiber stack to hold waiting threads.
   */

  /**
   * The run state of this task, initially NEW.  The run state
   * transitions to a terminal state only in methods set,
   * setException, and cancel.  During completion, state may take on
   * transient values of COMPLETING (while outcome is being set) or
   * INTERRUPTING (only while interrupting the runner to satisfy a
   * cancel(true)). Transitions from these intermediate to final
   * states use cheaper ordered/lazy writes because values are unique
   * and cannot be further modified.
   *
   * <pre>
   * Possible state transitions:
   * NEW -> COMPLETING -> NORMAL
   * NEW -> COMPLETING -> EXCEPTIONAL
   * NEW -> CANCELLED
   * NEW -> INTERRUPTING -> INTERRUPTED
   * </pre>
   */
  private volatile int state;

  private static final int NEW = 0;
  private static final int COMPLETING = 1;
  private static final int NORMAL = 2;
  private static final int EXCEPTIONAL = 3;
  private static final int CANCELLED = 4;
  private static final int INTERRUPTING = 5;
  private static final int INTERRUPTED = 6;

  /** The underlying callable; nulled out after running */

  @Nullable
  private Callable<V> callable;

  /** The result to return or exception to throw from get() */
  @Nullable
  private Object outcome; // non-volatile, protected by state reads/writes

  /** The thread running the callable; CASed during run() */
  @Nullable
  private volatile Thread runner;

  /** Treiber stack of waiting threads */
  @Nullable
  private volatile Waiter waiters;

  /**
   * Returns result or throws exception for completed task.
   *
   * @param s completed state value
   */
  @SuppressWarnings("unchecked")
  private V report(int s) throws ExecutionException {
    Object x = outcome;
    if (s == NORMAL)
      return (V) x;
    if (s >= CANCELLED)
      throw new CancellationException();
    throw new ExecutionException((Throwable) x);
  }

  /**
   * Create a new {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Callable}.
   *
   * @param callable the callable task
   * @param executor The {@link Executor} which is used to notify the {@code Future} once it is complete.
   */
  ListenableFutureTask(@Nullable Executor executor, Callable<V> callable) {
    super(executor);
    this.callable = callable;
    this.state = NEW;       // ensure visibility of callable
  }

  /**
   * Create a {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Runnable}, and arrange that {@link #get()}
   * will return the given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion
   * @param executor The {@link Executor} which is used to notify the {@code Future} once it is complete.
   */
  ListenableFutureTask(@Nullable Executor executor, Runnable runnable, @Nullable V result) {
    super(executor);
    this.callable = Executors.callable(runnable, result);
    this.state = NEW;       // ensure visibility of callable
  }

  @Override
  public boolean cancel() {
    return cancel(true);
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
  public boolean isCancellable() {
    return state == NEW;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (!(state == NEW && STATE.compareAndSet(this, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) {
      return false;
    }
    try {    // in case call to interrupt throws exception
      if (mayInterruptIfRunning) {
        try {
          Thread t = runner;
          if (t != null)
            t.interrupt();
        }
        finally { // final state
          STATE.setRelease(this, INTERRUPTED);
        }
      }
    }
    finally {
      finishCompletion();
    }
    return true;
  }

  @Override
  public Throwable getCause() {
    int s = this.state;
    if (s == EXCEPTIONAL) {
      return (Throwable) outcome;
    }
    else if (s >= CANCELLED) {
      return new DefaultFuture.LeanCancellationException();
    }
    return null;
  }

  @Override
  public @Nullable V getNow() {
    return null;
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
  public V get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
    int s = state;
    if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) {
      throw new TimeoutException();
    }
    return report(s);
  }

  @Override
  public ListenableFutureTask<V> sync() throws InterruptedException {
    await();
    rethrowIfFailed();
    return this;
  }

  @Override
  public ListenableFutureTask<V> syncUninterruptibly() {
    awaitUninterruptibly();
    rethrowIfFailed();
    return this;
  }

  @Override
  public ListenableFutureTask<V> await() throws InterruptedException {
    if (state <= COMPLETING) {
      awaitDone(false, 0L);
    }
    return this;
  }

  @Override
  public ListenableFutureTask<V> awaitUninterruptibly() {
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
    // noop
  }

  /**
   * Sets the result of this future to the given value unless
   * this future has already been set or has been cancelled.
   *
   * <p>This method is invoked internally by the {@link #run} method
   * upon successful completion of the computation.
   *
   * @param v the value
   */
  protected void set(V v) {
    if (STATE.compareAndSet(this, NEW, COMPLETING)) {
      outcome = v;
      STATE.setRelease(this, NORMAL); // final state
      finishCompletion();
    }
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
   */
  protected void setException(Throwable t) {
    if (STATE.compareAndSet(this, NEW, COMPLETING)) {
      outcome = t;
      STATE.setRelease(this, EXCEPTIONAL); // final state
      finishCompletion();
    }
  }

  @Override
  public void run() {
    if (state != NEW || !RUNNER.compareAndSet(this, null, Thread.currentThread())) {
      return;
    }
    try {
      Callable<V> c = callable;
      if (c != null && state == NEW) {
        V result;
        boolean ran;
        try {
          result = c.call();
          ran = true;
        }
        catch (Throwable ex) {
          result = null;
          ran = false;
          setException(ex);
        }
        if (ran)
          set(result);
      }
    }
    finally {
      // runner must be non-null until state is settled to
      // prevent concurrent calls to run()
      runner = null;
      // state must be re-read after nulling runner to prevent
      // leaked interrupts
      int s = state;
      if (s >= INTERRUPTING) {
        handlePossibleCancellationInterrupt(s);
      }
    }
  }

  /**
   * Ensures that any interrupt from a possible cancel(true) is only
   * delivered to a task while in run or runAndReset.
   */
  private void handlePossibleCancellationInterrupt(int s) {
    // It is possible for our interrupter to stall before getting a
    // chance to interrupt us.  Let's spin-wait patiently.
    if (s == INTERRUPTING)
      while (state == INTERRUPTING)
        Thread.yield(); // wait out pending interrupt

    // assert state == INTERRUPTED;

    // We want to clear any interrupt we may have received from
    // cancel(true).  However, it is permissible to use interrupts
    // as an independent mechanism for a task to communicate with
    // its caller, and there is no way to clear only the
    // cancellation interrupt.
    //
    // Thread.interrupted();
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
        for (; ; ) {
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
    callable = null;        // to reduce footprint
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
    for (; ; ) {
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
        if (timed && nanos <= 0L)
          return s;
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
        if (state < COMPLETING)
          LockSupport.parkNanos(this, parkNanos);
      }
      else
        LockSupport.park(this);
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
   *
   * @return a string representation of this FutureTask
   * @implSpec The default implementation returns a string identifying this
   * FutureTask, as well as its completion state.  The state, in
   * brackets, contains one of the strings {@code "Completed Normally"},
   * {@code "Completed Exceptionally"}, {@code "Cancelled"}, or {@code
   * "Not completed"}.
   */
  @Override
  public String toString() {
    final String status = switch (state) {
      case NORMAL -> "[Completed normally]";
      case EXCEPTIONAL -> "[Completed exceptionally: %s]".formatted(outcome);
      case CANCELLED, INTERRUPTING, INTERRUPTED -> "[Cancelled]";
      default -> {
        final Callable<?> callable = this.callable;
        yield callable == null ? "[Not completed]" : "[Not completed, task = %s]".formatted(callable);
      }
    };
    return super.toString() + status;
  }

  // VarHandle mechanics
  private static final VarHandle STATE;
  private static final VarHandle RUNNER;
  private static final VarHandle WAITERS;

  static {
    try {
      var l = MethodHandles.lookup();
      STATE = l.findVarHandle(ListenableFutureTask.class, "state", int.class);
      RUNNER = l.findVarHandle(ListenableFutureTask.class, "runner", Thread.class);
      WAITERS = l.findVarHandle(ListenableFutureTask.class, "waiters", Waiter.class);
    }
    catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

}

