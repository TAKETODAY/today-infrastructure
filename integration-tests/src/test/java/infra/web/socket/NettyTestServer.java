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

package infra.web.socket;

import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.annotation.config.web.netty.NettyWebServerFactoryAutoConfiguration;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.web.server.WebServer;
import infra.web.server.support.NettyWebServerFactory;
import infra.web.server.support.StandardNettyWebEnvironment;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/27 22:29
 */
public class NettyTestServer implements WebSocketTestServer {

  WebServer webServer;

  @Override
  public void setup(AnnotationConfigApplicationContext ctx) {
    ctx.setEnvironment(new StandardNettyWebEnvironment());
    ctx.register(NettyWebServerFactoryAutoConfiguration.class);
    ctx.register(ErrorMvcAutoConfiguration.class);
    ctx.register(WebMvcAutoConfiguration.class);
  }

  @Override
  public void start(AnnotationConfigApplicationContext ctx) {
    NettyWebServerFactory factory = ctx.getBean(NettyWebServerFactory.class);
    factory.setPort(0);

    webServer = factory.getWebServer();
    webServer.start();
  }

  @Override
  public void stop() {
    if (webServer != null) {
      webServer.stop();
    }
  }

  @Override
  public int getPort() {
    return webServer != null ? webServer.getPort() : -1;
  }

}
