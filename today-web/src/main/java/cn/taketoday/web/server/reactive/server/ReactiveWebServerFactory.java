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

package cn.taketoday.web.server.reactive.server;

import cn.taketoday.web.server.WebServer;
import cn.taketoday.web.server.WebServerFactory;
import cn.taketoday.http.server.reactive.HttpHandler;

/**
 * Factory interface that can be used to create a reactive {@link WebServer}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebServer
 * @since 4.0
 */
@FunctionalInterface
public interface ReactiveWebServerFactory extends WebServerFactory {

  /**
   * Gets a new fully configured but paused {@link WebServer} instance. Clients should
   * not be able to connect to the returned server until {@link WebServer#start()} is
   * called (which happens when the {@code ApplicationContext} has been fully
   * refreshed).
   *
   * @param httpHandler the HTTP handler in charge of processing requests
   * @return a fully configured and started {@link WebServer}
   * @see WebServer#stop()
   */
  WebServer getWebServer(HttpHandler httpHandler);

}
