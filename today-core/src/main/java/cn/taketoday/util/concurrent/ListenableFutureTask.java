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
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A cancellable asynchronous computation.
 * <p>
 * This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled.
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object. Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @param <V> the result type returned by this Future's {@code get} method
 * @author Doug Lea
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FutureTask
 * @since 4.0
 */
public class ListenableFutureTask<V> extends AbstractFuture<V> implements RunnableFuture<V> {

  /** The underlying callable; nulled out after running */

  @Nullable
  private Callable<V> task;

  /** The thread running the callable; CASed during run() */
  @Nullable
  private volatile Thread runner;

  /**
   * Create a new {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Callable}.
   *
   * @param task the callable task
   * @param executor The {@link Executor} which is used to notify the {@code Future} once it is complete.
   */
  ListenableFutureTask(@Nullable Executor executor, Callable<V> task) {
    super(executor);
    Assert.notNull(task, "task is required");
    this.task = task;
  }

  @Override
  public void run() {
    if (state == NEW && RUNNER.compareAndSet(this, null, Thread.currentThread())) {
      try {
        Callable<V> c = task;
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
            tryFailure(ex);
          }
          if (ran) {
            trySuccess(result);
          }
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
  }

  @Override
  protected void done() {
    super.done();
    task = null;        // to reduce footprint
  }

  @Override
  protected final void interruptTask() {
    try {
      Thread t = runner;
      if (t != null) {
        t.interrupt();
      }
    }
    finally { // final state
      super.interruptTask();
    }
  }

  /**
   * Ensures that any interrupt from a possible cancel(true) is only
   * delivered to a task while in run or runAndReset.
   */
  private void handlePossibleCancellationInterrupt(int s) {
    // It is possible for our interrupter to stall before getting a
    // chance to interrupt us.  Let's spin-wait patiently.
    if (s == INTERRUPTING) {
      while (state == INTERRUPTING) {
        Thread.yield(); // wait out pending interrupt
      }
    }
    // assert state == INTERRUPTED;

    // We want to clear any interrupt we may have received from
    // cancel(true).  However, it is permissible to use interrupts
    // as an independent mechanism for a task to communicate with
    // its caller, and there is no way to clear only the
    // cancellation interrupt.
    //
    // Thread.interrupted();
  }

  @Override
  protected String notCompletedString() {
    final Callable<?> task = this.task;
    return task == null ? "[Not completed]" : "[Not completed, task = %s]".formatted(task);
  }

  //

  @Override
  public ListenableFutureTask<V> sync() throws InterruptedException {
    super.sync();
    return this;
  }

  @Override
  public ListenableFutureTask<V> syncUninterruptibly() {
    super.syncUninterruptibly();
    return this;
  }

  @Override
  public ListenableFutureTask<V> await() throws InterruptedException {
    super.await();
    return this;
  }

  @Override
  public ListenableFutureTask<V> awaitUninterruptibly() {
    super.awaitUninterruptibly();
    return this;
  }

  @Override
  public ListenableFutureTask<V> onCompleted(FutureListener<? extends Future<V>> listener) {
    super.onCompleted(listener);
    return this;
  }

  @Override
  public <C> ListenableFutureTask<V> onCompleted(FutureContextListener<? extends Future<V>, C> listener, @Nullable C context) {
    super.onCompleted(listener, context);
    return this;
  }

  @Override
  public ListenableFutureTask<V> cascadeTo(final SettableFuture<V> settable) {
    Futures.cascade(this, settable);
    return this;
  }

  // VarHandle mechanics
  protected static final VarHandle RUNNER;

  static {
    try {
      RUNNER = MethodHandles.lookup().findVarHandle(ListenableFutureTask.class, "runner", Thread.class);
    }
    catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

}

