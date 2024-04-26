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

package cn.taketoday.web.server;

/**
 * A callback for the result of a graceful shutdown request.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebServer#shutDownGracefully(GracefulShutdownCallback)
 * @since 4.0
 */
@FunctionalInterface
public interface GracefulShutdownCallback {

  /**
   * Graceful shutdown has completed with the given {@code result}.
   *
   * @param result the result of the shutdown
   */
  void shutdownComplete(GracefulShutdownResult result);

}
