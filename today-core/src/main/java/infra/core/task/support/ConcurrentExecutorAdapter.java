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

package infra.core.task.support;

import java.util.concurrent.Executor;

import infra.core.task.TaskExecutor;
import infra.lang.Assert;
import infra.lang.NonNull;

/**
 * Adapter that exposes the {@link Executor} interface
 * for any {@link TaskExecutor}.
 *
 * <p>Since TaskExecutor itself extends the Executor interface.
 * The adapter is only relevant for <em>hiding</em> the TaskExecutor
 * nature of a given object now, solely exposing the standard
 * Executor interface to a client.
 *
 * @author Juergen Hoeller
 * @see Executor
 * @see TaskExecutor
 * @since 4.0
 */
public class ConcurrentExecutorAdapter implements Executor {

  private final TaskExecutor taskExecutor;

  /**
   * Create a new ConcurrentExecutorAdapter for the given TaskExecutor.
   *
   * @param taskExecutor the TaskExecutor to wrap
   */
  public ConcurrentExecutorAdapter(TaskExecutor taskExecutor) {
    Assert.notNull(taskExecutor, "TaskExecutor is required");
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void execute(@NonNull Runnable command) {
    this.taskExecutor.execute(command);
  }

}
