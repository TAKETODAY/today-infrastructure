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

package cn.taketoday.framework.web.server;

/**
 * The result of a graceful shutdown request.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GracefulShutdownCallback
 * @see WebServer#shutDownGracefully(GracefulShutdownCallback)
 * @since 4.0
 */
public enum GracefulShutdownResult {

  /**
   * Requests remained active at the end of the grace period.
   */
  REQUESTS_ACTIVE,

  /**
   * The server was idle with no active requests at the end of the grace period.
   */
  IDLE,

  /**
   * The server was shutdown immediately, ignoring any active requests.
   */
  IMMEDIATE

}
