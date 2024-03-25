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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import cn.taketoday.lang.Nullable;

/**
 * Listenable {@link FutureTask}
 *
 * @param <V> the result type returned by this Future's {@code get} method
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FutureTask
 * @since 4.0
 */
public class ListenableFutureTask<V> extends DefaultFuture<V> implements RunnableFuture<V> {

  private final FutureTask<V> futureTask;

  /**
   * Create a new {@code ListenableFutureTask} that will, upon running,
   * execute the given {@link Callable}.
   *
   * @param callable the callable task
   * @param executor The {@link Executor} which is used to notify the {@code Future} once it is complete.
   */
  ListenableFutureTask(@Nullable Executor executor, Callable<V> callable) {
    super(executor);
    this.futureTask = new FutureTaskAdapter(callable);
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
    this.futureTask = new FutureTaskAdapter(runnable, result);
  }

  @Override
  public void run() {
    futureTask.run();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return futureTask.cancel(mayInterruptIfRunning);
  }

  final class FutureTaskAdapter extends FutureTask<V> {

    public FutureTaskAdapter(Callable<V> callable) {
      super(callable);
    }

    public FutureTaskAdapter(Runnable runnable, @Nullable V result) {
      super(runnable, result);
    }

    @Override
    protected void done() {
      Throwable cause;
      try {
        V result = get();
        trySuccess(result);
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
      tryFailure(cause);
    }

  }

}
