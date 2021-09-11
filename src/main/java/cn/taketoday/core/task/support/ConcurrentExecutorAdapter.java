/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.core.task.support;

import java.util.concurrent.Executor;

import cn.taketoday.core.Assert;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.task.TaskExecutor;

/**
 * Adapter that exposes the {@link Executor} interface
 * for any {@link cn.taketoday.core.task.TaskExecutor}.
 *
 * <p>Since TaskExecutor itself extends the Executor interface.
 * The adapter is only relevant for <em>hiding</em> the TaskExecutor
 * nature of a given object now, solely exposing the standard
 * Executor interface to a client.
 *
 * @author Juergen Hoeller
 * @see Executor
 * @see cn.taketoday.core.task.TaskExecutor
 * @since 4.0
 */
public class ConcurrentExecutorAdapter implements Executor {

  private final TaskExecutor taskExecutor;

  /**
   * Create a new ConcurrentExecutorAdapter for the given TaskExecutor.
   *
   * @param taskExecutor
   *         the TaskExecutor to wrap
   */
  public ConcurrentExecutorAdapter(TaskExecutor taskExecutor) {
    Assert.notNull(taskExecutor, "TaskExecutor must not be null");
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void execute(@NonNull Runnable command) {
    this.taskExecutor.execute(command);
  }

}
