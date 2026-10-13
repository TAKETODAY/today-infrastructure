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

import infra.web.HttpContext;

/**
 * Executes HTTP request handling tasks dispatched from I/O worker threads.
 *
 * <p> Netty I/O worker threads delegate the blocking request handling logic, such as
 * database access, remote service calls, and complex computations, to this executor,
 * isolating it from high-performance I/O processing so that I/O threads are never
 * blocked and the framework's overall throughput and responsiveness stay unaffected.
 *
 * <p> The framework provides the {@link SimpleHttpRequestExecutor} which delegates
 * to a regular {@link java.util.concurrent.Executor}, and, on Java 21+, a virtual
 * thread based implementation.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/18 15:58
 */
public interface HttpRequestExecutor {

  /**
   * Executes the given command at some time in the future. The command
   * may execute in a new thread, in a pooled thread, or in the calling
   * thread, at the discretion of the {@code Executor} implementation.
   *
   * @param ctx the request context
   * @param command the runnable task
   * @throws IOException if an I/O error occurs during execution
   */
  void execute(HttpContext ctx, Runnable command) throws IOException;

}
