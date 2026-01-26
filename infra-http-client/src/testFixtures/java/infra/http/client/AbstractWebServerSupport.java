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

package infra.http.client;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.Http2;
import infra.web.server.WebServer;
import infra.web.server.netty.ChannelConfigurer;
import infra.web.server.netty.NettyWebServerFactory;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 18:19
 */
public abstract class AbstractWebServerSupport {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected NettyWebServerFactory createWebServerFactory() {
    NettyWebServerFactory factory = new NettyWebServerFactory();
    Http2 http2 = new Http2();
    http2.setEnabled(true);

    factory.setPort(0);
    factory.setHttp2(http2);
    factory.setChannelConfigurer(new ChannelConfigurer() { });
    factory.setHttpTrafficHandler(createHttpTrafficHandler());
    return factory;
  }

  protected abstract ChannelHandler createHttpTrafficHandler();

  protected GracefulShutdownCallback createGracefulShutdownCallback() {
    return result -> logger.debug("Graceful shutdown complete, result: {}", result);
  }

  protected void shutdownGracefully(WebServer webServer) {
    logger.debug("Shutting down Graceful shutdown...");
    webServer.shutdownGracefully(createGracefulShutdownCallback());
  }

}
