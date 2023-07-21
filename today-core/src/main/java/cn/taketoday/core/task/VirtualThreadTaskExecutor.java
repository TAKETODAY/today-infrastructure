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

package cn.taketoday.core.task;

import java.util.concurrent.ThreadFactory;

/**
 * A {@link TaskExecutor} implementation based on virtual threads in JDK 21+.
 * The only configuration option is a thread name prefix.
 *
 * <p>For additional features such as concurrency limiting or task decoration,
 * consider using {@link SimpleAsyncTaskExecutor#setVirtualThreads} instead.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleAsyncTaskExecutor
 * @since 4.0
 */
public class VirtualThreadTaskExecutor implements AsyncTaskExecutor {

  private final ThreadFactory virtualThreadFactory;

  /**
   * Create a new {@code VirtualThreadTaskExecutor} without thread naming.
   */
  public VirtualThreadTaskExecutor() {
    this.virtualThreadFactory = new VirtualThreadDelegate().virtualThreadFactory();
  }

  /**
   * Create a new {@code VirtualThreadTaskExecutor} with thread names based
   * on the given thread name prefix followed by a counter (e.g. "test-0").
   *
   * @param threadNamePrefix the prefix for thread names (e.g. "test-")
   */
  public VirtualThreadTaskExecutor(String threadNamePrefix) {
    this.virtualThreadFactory = new VirtualThreadDelegate().virtualThreadFactory(threadNamePrefix);
  }

  /**
   * Return the underlying virtual {@link ThreadFactory}.
   * Can also be used for custom thread creation elsewhere.
   */
  public final ThreadFactory getVirtualThreadFactory() {
    return this.virtualThreadFactory;
  }

  @Override
  public void execute(Runnable task) {
    this.virtualThreadFactory.newThread(task).start();
  }

}
