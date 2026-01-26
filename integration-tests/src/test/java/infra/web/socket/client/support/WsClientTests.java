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

package infra.web.socket.client.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import infra.app.Application;
import infra.app.InfraApplication;
import infra.core.io.buffer.DataBuffer;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.server.HandshakeCapable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/2 22:55
 */
class WsClientTests {

  static final Logger log = LoggerFactory.getLogger(WsClientTests.class);

  final NettyWebSocketClient client = new NettyWebSocketClient();

  @Test
  void doHandshake() throws InterruptedException {
    var context = Application.forBuilder(WsServerApp.class)
            .properties("server.port=8082", "server.http2.enabled=true")
            .run();
    CountDownLatch latch = new CountDownLatch(1);
    client.connect(new ClientWebSocketHandler(latch), "ws://localhost:8082/websocket")
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
    protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      assertThat(message.getPayloadAsText()).isEqualTo("Hello World");
      log.info("server handleTextMessage: {}", message);
      session.send(message.retain());
      session.send(factory -> {
        DataBuffer buffer = factory.copiedBuffer("hello");
        return WebSocketMessage.binary(buffer);
      });
      return Future.ok();
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
    protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      log.info("client handleTextMessage: {}", message);
      assertThat(message.getPayloadAsText()).isEqualTo("Hello World");
      latch.countDown();
      return Future.ok();
    }

  }

  @InfraApplication
  static class WsServerApp implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(new ServerWebSocketHandler(), "/websocket")
              .setAllowedOriginPatterns("*");
    }

  }

}
