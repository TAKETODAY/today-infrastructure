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

package infra.web.socket.client.support;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import infra.app.Application;
import infra.app.InfraApplication;
import infra.core.io.buffer.DataBuffer;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import infra.web.socket.config.EnableWebSocket;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.server.HandshakeCapable;
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
    var context = Application.run(WsServerApp.class);
    CountDownLatch latch = new CountDownLatch(1);
    client.connect(new ClientWebSocketHandler(latch), "ws://localhost:8080/websocket")
            .onSuccess(session -> session.sendText("Hello World"))
            .onFailure(Throwable::printStackTrace)
            .onFailure(e -> {
              latch.countDown();
              fail("connected failed");
            });

    latch.await();
    Application.exit(context);
  }

  static class ServerWebSocketHandler extends WebSocketHandler implements HandshakeCapable {

    @Override
    public boolean beforeHandshake(RequestContext request, Map<String, Object> attributes) throws Throwable {
      log.info("server beforeHandshake,{} {}", request, attributes);
      return HandshakeCapable.super.beforeHandshake(request, attributes);
    }

    @Override
    public void afterHandshake(RequestContext request, @Nullable WebSocketSession session, @Nullable Throwable failure) {
      log.info("server afterHandshake,{} {}", request, session);
    }

    @Override
    public void onOpen(WebSocketSession session) {
      log.info("server onOpen");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      assertThat(message.getPayloadAsText()).isEqualTo("Hello World");
      log.info("server handleTextMessage: {}", message);
      session.send(message.retain());
      session.send(factory -> {
        DataBuffer buffer = factory.copiedBuffer("hello");
        return WebSocketMessage.binary(buffer);
      });
    }

  }

  static class ClientWebSocketHandler extends WebSocketHandler {

    private final CountDownLatch latch;

    public ClientWebSocketHandler(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onOpen(WebSocketSession session) {
      log.info("client onOpen");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      log.info("client handleTextMessage: {}", message);
      assertThat(message.getPayloadAsText()).isEqualTo("Hello World");
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
