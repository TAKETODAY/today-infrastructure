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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.client.WebSocketClient;
import cn.taketoday.web.socket.config.EnableWebSocket;
import cn.taketoday.web.socket.config.WebSocketConfigurer;
import cn.taketoday.web.socket.config.WebSocketHandlerRegistry;
import cn.taketoday.web.socket.handler.TextWebSocketHandler;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Client and server-side WebSocket integration tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@Slf4j
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
    WebSocketSession session = this.webSocketClient.doHandshake(new TextWebSocketHandler(), headers, url).get();
    assertThat(session.getAcceptedProtocol()).isEqualTo("foo");
    session.close();
  }

  @ParameterizedWebSocketTest
  void unsolicitedPongWithEmptyPayload(WebSocketTestServer server,
          WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    super.setup(server, webSocketClient, testInfo);

    String url = getWsBaseUrl() + "/ws";
    WebSocketSession session = this.webSocketClient.doHandshake(new WebSocketHandler() { }, url).get();

    TestWebSocketHandler serverHandler = this.wac.getBean(TestWebSocketHandler.class);
    serverHandler.setWaitMessageCount(1);

    session.sendMessage(new PongMessage());

    serverHandler.await();
    assertThat(serverHandler.getTransportError()).isNull();
    assertThat(serverHandler.getReceivedMessages()).hasSize(1);
    assertThat(serverHandler.getReceivedMessages().get(0).getClass()).isEqualTo(PongMessage.class);
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
      log.error("handler: {}", handler);
      registry.addHandler(handler, "/ws")
              .setHandshakeHandler(handshakeHandler);
    }

    @Bean
    public TestWebSocketHandler handler() {
      return new TestWebSocketHandler();
    }

    @Bean
    public HandlerExceptionHandler handlerExceptionHandler() {
      return new HandlerExceptionHandler() {
        @Nullable
        @Override
        public Object handleException(RequestContext context,
                Throwable exception, @Nullable Object handler) throws Exception {
          log.error("出错啦", exception);
          return NONE_RETURN_VALUE;
        }
      };
    }

  }

  @SuppressWarnings("rawtypes")
  private static class TestWebSocketHandler extends WebSocketHandler {

    private final List<Message> receivedMessages = new ArrayList<>();

    private int waitMessageCount;

    private final CountDownLatch latch = new CountDownLatch(1);

    private Throwable transportError;

    public void setWaitMessageCount(int waitMessageCount) {
      this.waitMessageCount = waitMessageCount;
    }

    public List<Message> getReceivedMessages() {
      return this.receivedMessages;
    }

    public Throwable getTransportError() {
      return this.transportError;
    }

    @Override
    public void handleMessage(WebSocketSession session, Message<?> message) {
      this.receivedMessages.add(message);
      if (this.receivedMessages.size() >= this.waitMessageCount) {
        this.latch.countDown();
      }
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
