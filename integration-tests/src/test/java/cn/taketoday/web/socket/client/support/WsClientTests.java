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

package cn.taketoday.web.socket.client.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import cn.taketoday.framework.Application;
import cn.taketoday.framework.InfraApplication;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.config.EnableWebSocket;
import cn.taketoday.web.socket.config.WebSocketConfigurer;
import cn.taketoday.web.socket.config.WebSocketHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/2 22:55
 */
@Slf4j
class WsClientTests {
  final NettyWebSocketClient client = new NettyWebSocketClient();

  @Test
  void doHandshake() throws InterruptedException {
    Application.run(WsServerApp.class);

    CountDownLatch latch = new CountDownLatch(1);
    Future<WebSocketSession> sessionFuture = client.doHandshake(new ClientWebSocketHandler(latch), "ws://localhost:8080/websocket");

    sessionFuture.onSuccess(session -> session.sendText("Hello World"))
            .onFailure(Throwable::printStackTrace)
            .onFailure(e -> {
              latch.countDown();
              fail("connected failed");
            });

    latch.await();
  }

  static class ServerWebSocketHandler extends WebSocketHandler {

    @Override
    public void afterHandshake(RequestContext request, @Nullable WebSocketSession session) throws Throwable {
      log.info("server afterHandshake,{} {}", request, session);
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
      log.info("server onOpen");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      assertThat(message.getPayload()).isEqualTo("Hello World");
      log.info("server handleTextMessage: {}", message);
      session.sendMessage(message);
    }

  }

  static class ClientWebSocketHandler extends WebSocketHandler {

    private final CountDownLatch latch;

    public ClientWebSocketHandler(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onOpen(WebSocketSession session) throws Exception {
      log.info("client onOpen");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      log.info("client handleTextMessage: {}", message);
      assertThat(message.getPayload()).isEqualTo("Hello World");
      latch.countDown();
    }

  }

  @EnableWebSocket
  @InfraApplication
  static class WsServerApp implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(new ServerWebSocketHandler(), "/websocket")
              .setAllowedOriginPatterns("*");
    }

  }

}
