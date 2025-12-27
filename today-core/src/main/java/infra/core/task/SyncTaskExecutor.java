/*
 * Copyright 2017 - 2025 the original author or authors.
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
package infra.core.task;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.lang.Assert;
import infra.util.ConcurrencyThrottleSupport;

/**
 * {@link TaskExecutor} implementation that executes each task <i>synchronously</i>
 * in the calling thread.
 *
 * <p>Mainly intended for testing scenarios.
 *
 * <p>Execution in the calling thread does have the advantage of participating
 * in it's thread context, for example the thread context class loader or the
 * thread's current transaction association. That said, in many cases,
 * asynchronous execution will be preferable: choose an asynchronous
 * {@code TaskExecutor} instead for such scenarios.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SimpleAsyncTaskExecutor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SyncTaskExecutor extends ConcurrencyThrottleSupport implements TaskExecutor, Serializable {

  /**
   * Execute the given {@code task} synchronously, through direct
   * invocation of its {@link Runnable#run() run()} method.
   *
   * @throws RuntimeException if propagated from the given {@code Runnable}
   */
  @Override
  public void execute(Runnable task) {
    Assert.notNull(task, "Task is required");
    if (isThrottleActive()) {
      beforeAccess();
      try {
        task.run();
      }
      finally {
        afterAccess();
      }
    }
    else {
      task.run();
    }
  }

  /**
   * Execute the given {@code task} synchronously, through direct
   * invocation of its {@link TaskCallback#call() call()} method.
   *
   * @param <V> the returned value type, if any
   * @param <E> the exception propagated, if any
   * @throws E if propagated from the given {@code TaskCallback}
   * @since 5.0
   */
  public <V extends @Nullable Object, E extends Exception> V execute(TaskCallback<V, E> task) throws E {
    Assert.notNull(task, "Task is required");
    if (isThrottleActive()) {
      beforeAccess();
      try {
        return task.call();
      }
      finally {
        afterAccess();
      }
    }
    else {
      return task.call();
    }
  }

}
