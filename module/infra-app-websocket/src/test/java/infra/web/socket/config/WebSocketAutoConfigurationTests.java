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

package infra.web.socket.config;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.stereotype.Component;
import infra.web.server.context.AnnotationConfigWebServerApplicationContext;
import infra.web.server.netty.RandomPortWebServerConfig;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.RequestUpgradeStrategy;
import infra.web.socket.server.support.WebSocketHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/28 22:37
 */
class WebSocketAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = ApplicationContextRunner.forProvider(AnnotationConfigWebServerApplicationContext::new)
          .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class, WebSocketAutoConfiguration.class))
          .withUserConfiguration(TestConfig.class);

  @Test
  void beans() {
    this.contextRunner.run(context -> {
      assertThat(context).hasSingleBean(WebSocketHandler.class);
      assertThat(context).hasSingleBean(WebSocketConfigurer.class);
      assertThat(context).hasSingleBean(WebSocketHandlerMapping.class);
      assertThat(context).hasSingleBean(RequestUpgradeStrategy.class);
      assertThat(context).hasBean("wsHandler");
      assertThat(context).hasBean("wsConfigurer");
      assertThat(context).hasBean("nettyRequestUpgradeStrategy");
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Component
    static WsConfigurer wsConfigurer(WsHandler wsHandler) {
      return new WsConfigurer(wsHandler);
    }

    @Component
    static WsHandler wsHandler() {
      return new WsHandler();
    }

  }

  static class WsConfigurer implements WebSocketConfigurer {
    final WsHandler wsHandler;

    WsConfigurer(WsHandler wsHandler) {
      this.wsHandler = wsHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(wsHandler, "/ws");
    }

  }

  static class WsHandler extends WebSocketHandler {

    @Override
    public void onOpen(WebSocketSession session) throws Exception {

    }

  }

}