/*
 * Copyright 2017 - 2026 the TODAY authors.
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
   * Executes the given command at some time in the future. The command
   * may execute in a new thread, in a pooled thread, or in the calling
   * thread, at the discretion of the {@code Executor} implementation.
   *
   * @param ctx the request context
   * @param command the runnable task
   * @throws IOException if an I/O error occurs during execution
   */
  void execute(RequestContext ctx, Runnable command) throws IOException;

}
