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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import infra.http.HttpStatus;
import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Simple {@link ServiceExecutor} implementation that delegates to an {@link Executor}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/18 15:59
 */
public class SimpleServiceExecutor implements ServiceExecutor {

  private final Executor executor;

  /**
   * Create a new SimpleServiceExecutor instance.
   *
   * @param executor the executor to delegate to
   */
  public SimpleServiceExecutor(Executor executor) {
    Assert.notNull(executor, "Executor is required");
    this.executor = executor;
  }

  /**
   * Execute the given command using the configured executor.
   * If the executor rejects the task, set status to 503 SERVICE UNAVAILABLE.
   *
   * @param ctx the request context
   * @param command the command to execute
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void execute(RequestContext ctx, Runnable command) throws IOException {
    try {
      executor.execute(command);
    }
    catch (RejectedExecutionException e) {
      ctx.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
      ctx.flush();
    }
  }

}
