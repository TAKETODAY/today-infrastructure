/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;

/**
 * Adapter that takes a {@link cn.taketoday.core.task.TaskExecutor}
 * and exposes a full {@code java.util.concurrent.ExecutorService} for it.
 *
 * <p>This is primarily for adapting to client components that communicate via the
 * {@code java.util.concurrent.ExecutorService} API. It can also be used as
 * common ground between a local {@code TaskExecutor} backend and a
 * JNDI-located {@code ManagedExecutorService} in a Java EE 7 environment.
 *
 * <p><b>NOTE:</b> This ExecutorService adapter does <em>not</em> support the
 * lifecycle methods in the {@code java.util.concurrent.ExecutorService} API
 * ("shutdown()" etc), similar to a server-wide {@code ManagedExecutorService}
 * in a Java EE 7 environment. The lifecycle is always up to the backend pool,
 * with this adapter acting as an access-only proxy for that target pool.
 *
 * @author Juergen Hoeller
 * @see java.util.concurrent.ExecutorService
 * @since 4.0
 */
public class ExecutorServiceAdapter extends AbstractExecutorService {

  private final TaskExecutor taskExecutor;

  /**
   * Create a new ExecutorServiceAdapter, using the given target executor.
   *
   * @param taskExecutor the target executor to delegate to
   */
  public ExecutorServiceAdapter(TaskExecutor taskExecutor) {
    Assert.notNull(taskExecutor, "TaskExecutor is required");
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void execute(@NonNull Runnable task) {
    this.taskExecutor.execute(task);
  }

  @Override
  public void shutdown() {
    throw new IllegalStateException(
            "Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new IllegalStateException(
            "Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
  }

  @Override
  public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) {
    throw new IllegalStateException(
            "Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  // @Override on JDK 19
  public void close() {
    // no-op in order to avoid container-triggered shutdown call which would lead to exception logging
  }

}
