/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.web.servlet.server;

import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.server.WebServerFactory;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;

/**
 * Factory interface that can be used to create a {@link WebServer}.
 *
 * @author Phillip Webb
 * @see WebServer
 * @since 4.0
 */
@FunctionalInterface
public interface ServletWebServerFactory extends WebServerFactory {

  /**
   * Gets a new fully configured but paused {@link WebServer} instance. Clients should
   * not be able to connect to the returned server until {@link WebServer#start()} is
   * called (which happens when the {@code ApplicationContext} has been fully
   * refreshed).
   *
   * @param initializers {@link ServletContextInitializer}s that should be applied as
   * the server starts
   * @return a fully configured and started {@link WebServer}
   * @see WebServer#stop()
   */
  WebServer getWebServer(ServletContextInitializer... initializers);

}
