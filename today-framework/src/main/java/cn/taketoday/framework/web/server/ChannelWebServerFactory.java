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

import cn.taketoday.framework.web.netty.NettyChannelHandler;

/**
 * Factory interface that can be used to create a netty {@link WebServer}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/25 16:46
 */
public interface ChannelWebServerFactory extends WebServerFactory {

  /**
   * Gets a new fully configured but paused {@link WebServer} instance. Clients should
   * not be able to connect to the returned server until {@link WebServer#start()} is
   * called (which happens when the {@code ApplicationContext} has been fully
   * refreshed).
   *
   * @param channelHandler the HTTP handler in charge of processing requests
   * @return a fully configured and started {@link WebServer}
   * @see WebServer#stop()
   */
  WebServer getWebServer(NettyChannelHandler channelHandler);

}
