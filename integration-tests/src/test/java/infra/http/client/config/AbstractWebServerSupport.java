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

package infra.http.client.config;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.WebServer;
import infra.web.server.netty.ChannelConfigurer;
import infra.web.server.netty.NettyWebServerFactory;
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
