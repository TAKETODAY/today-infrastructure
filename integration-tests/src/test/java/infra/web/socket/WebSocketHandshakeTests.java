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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.util.concurrent.Future;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;
import infra.web.socket.client.WebSocketClient;
import infra.web.socket.config.EnableWebSocket;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.handler.TextWebSocketHandler;
import infra.web.socket.server.support.DefaultHandshakeHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Client and server-side WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class WebSocketHandshakeTests extends AbstractWebSocketIntegrationTests {

  @Override
  protected Class<?>[] getAnnotatedConfigClasses() {
    return new Class<?>[] { TestConfig.class };
  }

  @ParameterizedWebSocketTest
  void subProtocolNegotiation(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    super.setup(server, webSocketClient, testInfo);

    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.setSecWebSocketProtocol("foo");
    URI url = URI.create(getWsBaseUrl() + "/ws");
    WebSocketSession session = this.webSocketClient.connect(url, headers, new TextWebSocketHandler()).await().obtain();
    assertThat(session.getAcceptedProtocol()).isEqualTo("foo");
    session.close();
  }

  @ParameterizedWebSocketTest
  void unsolicitedPongWithEmptyPayload(WebSocketTestServer server,
          WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    super.setup(server, webSocketClient, testInfo);

    String url = getWsBaseUrl() + "/ws";
    WebSocketSession session = this.webSocketClient.connect(new WebSocketHandler() { }, url).await().obtain();

    TestWebSocketHandler serverHandler = this.ctx.getBean(TestWebSocketHandler.class);
    serverHandler.setWaitMessageCount(1);

    session.sendPong();

    serverHandler.await();
    assertThat(serverHandler.getTransportError()).isNull();
    assertThat(serverHandler.getReceivedMessages()).hasSize(1);
    assertThat(serverHandler.getReceivedMessages().get(0).getType()).isEqualTo(WebSocketMessage.Type.PONG);
  }

  @Configuration
  @EnableWebSocket
  static class TestConfig implements WebSocketConfigurer {

    @Autowired
    private DefaultHandshakeHandler handshakeHandler;  // can't rely on classpath for server detection

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      handshakeHandler.setSupportedProtocols("foo", "bar", "baz");
      TestWebSocketHandler handler = handler();
      registry.addHandler(handler, "/ws")
              .setAllowedOriginPatterns("*")
              .setHandshakeHandler(handshakeHandler);
    }

    @Bean
    public TestWebSocketHandler handler() {
      return new TestWebSocketHandler();
    }

    @Bean
    public HandlerExceptionHandler handlerExceptionHandler() {
      return new HandlerExceptionHandler() {

        @Override
        public Object handleException(RequestContext context,
                Throwable exception, @Nullable Object handler) throws Exception {
          return NONE_RETURN_VALUE;
        }
      };
    }

  }

  private static class TestWebSocketHandler extends WebSocketHandler {

    private final List<WebSocketMessage> receivedMessages = new ArrayList<>();

    private int waitMessageCount;

    private final CountDownLatch latch = new CountDownLatch(1);

    private Throwable transportError;

    public void setWaitMessageCount(int waitMessageCount) {
      this.waitMessageCount = waitMessageCount;
    }

    public List<WebSocketMessage> getReceivedMessages() {
      return this.receivedMessages;
    }

    public Throwable getTransportError() {
      return this.transportError;
    }

    @Nullable
    @Override
    public Future<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
      this.receivedMessages.add(message);
      if (this.receivedMessages.size() >= this.waitMessageCount) {
        this.latch.countDown();
      }
      return null;
    }

    @Override
    public void onError(WebSocketSession session, Throwable throwable) {
      this.transportError = throwable;
      this.latch.countDown();
    }

    public void await() throws InterruptedException {
      this.latch.await(2, TimeUnit.SECONDS);
    }
  }

}
