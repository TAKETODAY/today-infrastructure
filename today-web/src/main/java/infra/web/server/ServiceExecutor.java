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

package infra.web.server;

import java.io.IOException;

import infra.web.RequestContext;

/**
 * ServiceExecutor is a dedicated independent thread pool within the framework
 * designed to execute blocking business logic.
 *
 * <p> It receives tasks dispatched from
 * Netty I/O Worker threads and handles all core business operations, such as database access,
 * remote service calls, and complex computations. Its purpose is to isolate potentially
 * blocking business operations from high-performance I/O processing threads, preventing
 * I/O threads from becoming blocked and thereby impacting the framework's overall
 * throughput and responsiveness. It is recommended to use Netty's DefaultEventExecutorGroup
 * implementation for seamless integration with the Pipeline and to ensure task sequencing.
 * Additionally, comprehensive thread pool parameter configuration, exception handling,
 * and monitoring mechanisms must be implemented to guarantee stable and efficient business processing.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/18 15:58
 */
public interface ServiceExecutor {

  /**
   * Executes the given command at some time in the future.  The command
   * may execute in a new thread, in a pooled thread, or in the calling
   * thread, at the discretion of the {@code Executor} implementation.
   *
   * @param command the runnable task
   */
  void execute(RequestContext ctx, Runnable command) throws IOException;

}
