/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket;

import org.junit.jupiter.api.TestInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.web.socket.client.WebSocketClient;
import cn.taketoday.web.socket.config.EnableWebSocket;
import cn.taketoday.web.socket.config.WebSocketConfigurer;
import cn.taketoday.web.socket.config.WebSocketHandlerRegistry;
import cn.taketoday.web.socket.server.HandshakeHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WebSocket Java server-side configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class WebSocketConfigurationTests extends AbstractWebSocketIntegrationTests {

  @Override
  protected Class<?>[] getAnnotatedConfigClasses() {
    return new Class<?>[] { TestConfig.class };
  }

  @ParameterizedWebSocketTest
  void registerWebSocketHandler(WebSocketTestServer server,
          WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    super.setup(server, webSocketClient, testInfo);

    WebSocketSession session = this.webSocketClient.doHandshake(
            new WebSocketHandler() { }, getWsBaseUrl() + "/ws").get();

    TestHandler serverHandler = this.wac.getBean(TestHandler.class);
    assertThat(serverHandler.connectLatch.await(2, TimeUnit.SECONDS)).isTrue();

    session.close();
  }

  @Configuration
  @EnableWebSocket
  static class TestConfig implements WebSocketConfigurer {

    @Autowired
    private HandshakeHandler handshakeHandler; // can't rely on classpath for server detection

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(serverHandler(), "/ws")
              .setHandshakeHandler(this.handshakeHandler);
    }

    @Bean
    public TestHandler serverHandler() {
      return new TestHandler();
    }
  }

  private static class TestHandler extends WebSocketHandler {

    private CountDownLatch connectLatch = new CountDownLatch(1);

    @Override
    public void onOpen(WebSocketSession session) {
      this.connectLatch.countDown();
    }
  }

}
