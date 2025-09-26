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

import org.junit.jupiter.api.TestInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.lang.Nullable;
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
