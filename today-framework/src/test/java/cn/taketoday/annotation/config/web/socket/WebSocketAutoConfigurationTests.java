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

package cn.taketoday.annotation.config.web.socket;

import org.junit.jupiter.api.Test;

import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.server.context.AnnotationConfigWebServerApplicationContext;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.config.WebSocketConfigurer;
import cn.taketoday.web.socket.config.WebSocketHandlerRegistry;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;

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