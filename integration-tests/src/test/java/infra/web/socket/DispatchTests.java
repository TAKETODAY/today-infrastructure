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

package infra.web.socket;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import infra.app.InfraApplication;
import infra.app.test.context.InfraTest;
import infra.app.test.web.server.LocalServerPort;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ActiveProfiles;
import infra.test.context.TestPropertySource;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.socket.client.support.NettyWebSocketClient;
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
    String generated = generateRandomString(1024);
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

    @Nullable
    @Override
    protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      messages.add(message.getPayloadAsText());
      dataLatch.countDown();
      return null;
    }

    @Nullable
    @Override
    public Future<Void> onClose(WebSocketSession session, CloseStatus status) {
      latch.countDown();
      return null;
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
    public void onOpen(WebSocketSession session) {
      sessions.add(session);
    }

    @Nullable
    @Override
    protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      return Future.combine(sessions.stream().filter(item -> item != session)
              .map(current -> current.send(message.retainedDuplicate()))).asVoid();
    }

    @Nullable
    @Override
    public Future<Void> onClose(WebSocketSession session, CloseStatus status) {
      sessions.remove(session);
      return null;
    }
  }

  @InfraApplication
  static class WsServerApp implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(new LoggingWebSocketHandler(new ServerHandler()), "/websocket")
              .setAllowedOriginPatterns("*");
    }

  }

  private static final Random random = new Random();

  public static String generateRandomString(int length) {
    final char[] ret = new char[length];
    for (int i = 0; i < length; i++) {
      ret[i] = generateRandomCharacter(random.nextInt(3));
    }
    return String.valueOf(ret);
  }

  private static char generateRandomCharacter(int type) {
    int rand;
    switch (type) {
      case 0 -> {//随机小写字母
        rand = random.nextInt(26);
        rand += 97;
        return (char) rand;
      }
      case 1 -> {//随机大写字母
        rand = random.nextInt(26);
        rand += 65;
        return (char) rand;
      }//随机数字
      default -> {
        rand = random.nextInt(10);
        rand += 48;
        return (char) rand;
      }
    }
  }
  // 3.0

}
