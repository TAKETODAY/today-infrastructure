/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.WebServer;
import infra.web.server.support.ChannelConfigurer;
import infra.web.server.support.NettyWebServerFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 18:19
 */
public abstract class AbstractWebServerSupport {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected NettyWebServerFactory createWebServerFactory() {
    NettyWebServerFactory webServerFactory = new NettyWebServerFactory();
    webServerFactory.setPort(0);
    webServerFactory.setChannelConfigurer(new ChannelConfigurer() {

      @Override
      public void postInitChannel(Channel ch) {
        ch.pipeline().addAfter("HttpServerCodec", "httpObjectAggregator",
                new HttpObjectAggregator(1000, true));
      }
    });
    webServerFactory.setChannelHandler(createChannelHandler());
    return webServerFactory;
  }

  protected abstract ChannelHandler createChannelHandler();

  protected GracefulShutdownCallback createGracefulShutdownCallback() {
    return result -> logger.debug("Graceful shutdown complete, result: {}", result);
  }

  protected void shutdownGracefully(WebServer webServer) {
    logger.debug("Shutting down Graceful shutdown...");
    webServer.shutDownGracefully(createGracefulShutdownCallback());
  }

}
