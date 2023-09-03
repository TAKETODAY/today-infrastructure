/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.task.support;

import cn.taketoday.core.task.TaskDecorator;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;

/**
 * {@link TaskDecorator} that {@link ContextSnapshot#wrap(Runnable) wrap the execution} of
 * tasks, assisting with context propagation.
 * <p>This operation is only useful when the task execution is scheduled on a different
 * thread than the original call stack; this depends on the choice of
 * {@link cn.taketoday.core.task.TaskExecutor}. This is particularly useful for
 * restoring a logging context or an observation context for the task execution. Note that
 * this decorator will cause some overhead for task execution and is not recommended for
 * applications that run lots of very small tasks.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CompositeTaskDecorator
 * @since 4.0 2023/9/3 12:53
 */
public class ContextPropagatingTaskDecorator implements TaskDecorator {

  private final ContextSnapshotFactory factory;

  /**
   * Create a new decorator that uses a default instance of the {@link ContextSnapshotFactory}.
   */
  public ContextPropagatingTaskDecorator() {
    this(ContextSnapshotFactory.builder().build());
  }

  /**
   * Create a new decorator using the given {@link ContextSnapshotFactory}.
   *
   * @param factory the context snapshot factory to use.
   */
  public ContextPropagatingTaskDecorator(ContextSnapshotFactory factory) {
    this.factory = factory;
  }

  @Override
  public Runnable decorate(Runnable runnable) {
    return this.factory.captureAll().wrap(runnable);
  }

}
