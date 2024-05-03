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

package cn.taketoday.web.socket;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.web.server.WebServer;
import cn.taketoday.web.server.support.NettyRequestConfig;
import cn.taketoday.web.server.support.NettyWebServerFactory;
import cn.taketoday.web.socket.server.support.WsNettyChannelHandler;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/27 22:29
 */
public class NettyTestServer implements WebSocketTestServer {

  WebServer webServer;

  WsNettyChannelHandler channelHandler;

  @Override
  public void setup(AnnotationConfigApplicationContext wac) {
    NettyRequestConfig requestConfig = NettyRequestConfig.forBuilder()
            .httpDataFactory(new DefaultHttpDataFactory())
            .sendErrorHandler((request, message) -> {

            })
            .build();

    channelHandler = new WsNettyChannelHandler(requestConfig, wac);
  }

  @Override
  public void start() throws Exception {
    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setPort(0);

    channelHandler.init();
    webServer = factory.getWebServer(channelHandler);
    webServer.start();
  }

  @Override
  public void stop() throws Exception {
    if (webServer != null) {
      webServer.stop();
    }
  }

  @Override
  public int getPort() {
    return webServer != null ? webServer.getPort() : -1;
  }

}
