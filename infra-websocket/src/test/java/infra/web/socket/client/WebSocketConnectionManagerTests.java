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

package infra.web.socket.client;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import infra.context.Lifecycle;
import infra.context.SmartLifecycle;
import infra.http.HttpHeaders;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;
import infra.web.socket.handler.LoggingWebSocketHandler;
import infra.web.socket.handler.TextWebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture for {@link WebSocketConnectionManager}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketConnectionManagerTests {

  @Test
  public void openConnection() throws Exception {
    List<String> subprotocols = Arrays.asList("abc");

    TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
    WebSocketHandler handler = new TextWebSocketHandler();

    WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler, "/path/{id}", "123");
    manager.setSubProtocols(subprotocols);
    manager.openConnection();

    WebSocketHttpHeaders expectedHeaders = new WebSocketHttpHeaders();
    expectedHeaders.setSecWebSocketProtocol(subprotocols);

    assertThat(client.headers).isEqualTo(expectedHeaders);
    assertThat(client.uri).isEqualTo(new URI("/path/123"));

    WebSocketHandler loggingHandler = client.webSocketHandler;
    assertThat(loggingHandler.getClass()).isEqualTo(LoggingWebSocketHandler.class);

    assertThat(loggingHandler.getRawHandler()).isSameAs(handler);
  }

  @Test
  public void clientLifecycle() throws Exception {
    TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
    WebSocketHandler handler = new TextWebSocketHandler();
    WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler, "/a");

    manager.startInternal();
    assertThat(client.isRunning()).isTrue();

    manager.stopInternal();
    assertThat(client.isRunning()).isFalse();
  }

  @Test
  void startsOnceAndClosesEstablishedSessionBeforeInvokingStopCallback() {
    URI uri = URI.create("ws://example.test/socket");
    WebSocketClient client = mock(WebSocketClient.class);
    Promise<WebSocketSession> result = Future.forPromise(Runnable::run);
    given(client.connect(eq(uri), any(HttpHeaders.class), any(WebSocketHandler.class))).willReturn(result);
    WebSocketConnectionManager manager = new WebSocketConnectionManager(client, new TextWebSocketHandler(), uri);
    WebSocketSession session = mock(WebSocketSession.class);
    given(session.isOpen()).willReturn(true);

    assertThat(manager.isAutoStartup()).isFalse();
    assertThat(manager.getPhase()).isEqualTo(SmartLifecycle.DEFAULT_PHASE);
    manager.setAutoStartup(true);
    manager.setPhase(10);
    assertThat(manager.isAutoStartup()).isTrue();
    assertThat(manager.getPhase()).isEqualTo(10);

    manager.stop();
    verifyNoInteractions(client);
    manager.start();
    manager.start();
    verify(client).connect(eq(uri), any(HttpHeaders.class), any(LoggingWebSocketHandler.class));
    assertThat(manager.isRunning()).isTrue();
    assertThat(manager.isConnected()).isFalse();
    result.setSuccess(session);
    assertThat(manager.isConnected()).isTrue();

    Runnable callback = mock(Runnable.class);
    manager.stop(() -> {
      verify(session).close();
      assertThat(manager.isRunning()).isFalse();
      callback.run();
    });
    manager.stop();
    verify(callback).run();
    verify(session).close();
  }

  @Test
  void doesNotRestartRunningClientOrCloseClosedSession() {
    TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(true);
    WebSocketConnectionManager manager = new WebSocketConnectionManager(client, new TextWebSocketHandler(), "/socket");
    WebSocketSession session = mock(WebSocketSession.class);
    Logger logger = (Logger) LoggerFactory.getLogger(WebSocketConnectionManager.class);
    Level previous = logger.getLevel();
    logger.setLevel(Level.WARN);
    try {
      manager.start();
      manager.operationComplete(Future.ok(session));
      assertThat(manager.isConnected()).isFalse();
      manager.stop();
      verify(session, times(2)).isOpen();
      verifyNoMoreInteractions(session);

      manager.start();
      manager.operationComplete(Future.failed(new IllegalStateException("handshake rejected")));
      manager.stop();
      assertThat(manager.isRunning()).isFalse();
    }
    finally {
      logger.setLevel(previous);
    }
  }

  @Test
  void stopStillInvokesCallbackWhenClosingSessionThrows() throws Exception {
    TestLifecycleWebSocketClient client = new TestLifecycleWebSocketClient(false);
    WebSocketConnectionManager manager = new WebSocketConnectionManager(client, new TextWebSocketHandler(), "/socket");
    manager.closeConnection();
    WebSocketSession session = mock(WebSocketSession.class);
    given(session.isOpen()).willReturn(true);
    given(session.close()).willThrow(new IllegalStateException("close failed"));
    manager.start();
    manager.operationComplete(Future.ok(session));
    client.stop();
    Runnable callback = mock(Runnable.class);

    manager.stop(callback);

    assertThat(manager.isRunning()).isFalse();
    verify(callback).run();
  }

  @Test
  void configuresHandshakeHeaders() {
    WebSocketConnectionManager manager = new WebSocketConnectionManager(mock(WebSocketClient.class),
            new TextWebSocketHandler(), "/socket");
    manager.setOrigin("https://example.test");
    manager.setSubProtocols(List.of("chat"));
    assertThat(manager.getOrigin()).isEqualTo("https://example.test");
    assertThat(manager.getSubProtocols()).containsExactly("chat");

    HttpHeaders replacement = HttpHeaders.forWritable();
    replacement.set("X-Test", "value");
    manager.setHeaders(replacement);
    assertThat(manager.getHeaders().getFirst("X-Test")).isEqualTo("value");
    assertThat(manager.getOrigin()).isNull();
    assertThat(manager.getSubProtocols()).isEmpty();
  }

  private static class TestLifecycleWebSocketClient implements WebSocketClient, Lifecycle {

    private boolean running;

    private WebSocketHandler webSocketHandler;

    private HttpHeaders headers;

    private URI uri;

    public TestLifecycleWebSocketClient(boolean running) {
      this.running = running;
    }

    @Override
    public void start() {
      this.running = true;
    }

    @Override
    public void stop() {
      this.running = false;
    }

    @Override
    public boolean isRunning() {
      return this.running;
    }

    @Override
    public Future<WebSocketSession> connect(URI uri, @Nullable HttpHeaders headers, WebSocketHandler handler) {

      this.webSocketHandler = handler;
      this.headers = headers;
      this.uri = uri;
      return Future.forFutureTask(() -> null);
    }

  }

}
