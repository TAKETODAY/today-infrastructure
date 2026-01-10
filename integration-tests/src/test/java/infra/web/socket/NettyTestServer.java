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
