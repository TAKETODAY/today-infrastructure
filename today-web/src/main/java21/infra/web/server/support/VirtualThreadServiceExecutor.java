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
