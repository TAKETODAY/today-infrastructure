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

package infra.web.server.support;

import infra.web.RequestContext;
import infra.web.server.ServiceExecutor;

/**
 * {@link ServiceExecutor} implementation that executes service methods
 * using virtual threads.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/17 15:59
 */
public class VirtualThreadServiceExecutor implements ServiceExecutor {

  /**
   * Execute the given command using a virtual thread.
   *
   * @param ctx the request context
   * @param command the command to execute
   */
  @Override
  public void execute(RequestContext ctx, Runnable command) {
    Thread.startVirtualThread(command);
  }

}
