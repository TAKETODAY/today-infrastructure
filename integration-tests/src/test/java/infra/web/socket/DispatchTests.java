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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import infra.app.InfraApplication;
import infra.app.test.context.InfraTest;
import infra.app.test.web.server.LocalServerPort;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ActiveProfiles;
import infra.test.context.TestPropertySource;
import infra.util.StringUtils;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.socket.client.support.NettyWebSocketClient;
import infra.web.socket.config.EnableWebSocket;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.handler.LoggingWebSocketHandler;
import infra.web.socket.server.HandshakeCapable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/12/16 14:23
 */
@TestPropertySource(properties = "logging.level.web=info")
@ActiveProfiles("dev")
@InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT)
class DispatchTests {

  static final Logger log = LoggerFactory.getLogger(DispatchTests.class);

  static final int connections = 32;

  final NettyWebSocketClient client = new NettyWebSocketClient();

  @LocalServerPort
  private int port;

  @Test
  void test() throws Exception {
    String generated = StringUtils.generateRandomString(1024);
    String path = "ws://localhost:%d/websocket".formatted(port);
    CountDownLatch latch = new CountDownLatch(1);
    CountDownLatch dataLatch = new CountDownLatch(connections);

    ArrayList<String> messages = new ArrayList<>();

    LoggingWebSocketHandler handler = new LoggingWebSocketHandler(new ClientHandler(latch, messages, dataLatch));

    List<Future<?>> futures = new ArrayList<>(connections);
    for (int i = 0; i < connections; i++) {
      futures.add(client.connect(handler, path));
    }

    var combiner = Future.combine(futures);

    client.connect(handler, path)
            .onSuccess(session -> {
              combiner.run(() -> {
                session.send(factory -> WebSocketMessage.text(factory.copiedBuffer(generated)))
                        .onSuccess(() -> log.info("send ok"));
              });

              dataLatch.await();
              session.close();
            })
            .onFailure(Throwable::printStackTrace)
            .onFailure(e -> {
              latch.countDown();
              fail("connected failed");
            });

    latch.await();
    client.shutdown();

    assertThat(messages).hasSize(connections);
    assertThat(new HashSet<>(messages)).hasSize(1);
  }

  static class ClientHandler extends WebSocketHandler {

    private final CountDownLatch latch;

    private final List<String> messages;

    private final CountDownLatch dataLatch;

    public ClientHandler(CountDownLatch latch, List<String> messages, CountDownLatch dataLatch) {
      this.latch = latch;
      this.messages = messages;
      this.dataLatch = dataLatch;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      messages.add(message.getPayloadAsText());
      dataLatch.countDown();
    }

    @Override
    public void onClose(WebSocketSession session, CloseStatus status) {
      latch.countDown();
    }

  }

  static class ServerHandler extends WebSocketHandler implements HandshakeCapable {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public boolean beforeHandshake(RequestContext request, Map<String, Object> attributes) throws Throwable {
      return HandshakeCapable.super.beforeHandshake(request, attributes);
    }

    @Override
    public void afterHandshake(RequestContext request, @Nullable WebSocketSession session, @Nullable Throwable failure) throws Throwable {
      HandshakeCapable.super.afterHandshake(request, session, failure);
    }

    @Override
    public void onOpen(WebSocketSession session) throws Throwable {
      super.onOpen(session);
      sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      for (WebSocketSession current : sessions) {
        if (session != current) {
          current.send(message.retainedDuplicate());
        }
      }
    }

    @Override
    public void onClose(WebSocketSession session, CloseStatus status) throws Throwable {
      super.onClose(session, status);
      sessions.remove(session);
    }
  }

  @EnableWebSocket
  @InfraApplication
  static class WsServerApp implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(new LoggingWebSocketHandler(new ServerHandler()), "/websocket")
              .setAllowedOriginPatterns("*");
    }

  }

}
