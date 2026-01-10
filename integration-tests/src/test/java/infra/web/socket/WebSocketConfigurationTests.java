/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket;

import org.junit.jupiter.api.TestInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.web.socket.client.WebSocketClient;
import infra.web.socket.config.EnableWebSocket;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.server.HandshakeHandler;

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
  void registerWebSocketHandler(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    super.setup(server, webSocketClient, testInfo);

    WebSocketSession session = this.webSocketClient.connect(
            new WebSocketHandler() { }, getWsBaseUrl() + "/ws").get();

    TestHandler serverHandler = this.ctx.getBean(TestHandler.class);
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
              .setAllowedOriginPatterns("*")
              .setHandshakeHandler(this.handshakeHandler);
    }

    @Bean
    public TestHandler serverHandler() {
      return new TestHandler();
    }
  }

  private static class TestHandler extends WebSocketHandler {

    private final CountDownLatch connectLatch = new CountDownLatch(1);

    @Override
    public void onOpen(WebSocketSession session) {
      this.connectLatch.countDown();
    }
  }

}
