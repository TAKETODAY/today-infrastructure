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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import infra.context.Lifecycle;
import infra.http.HttpHeaders;
import infra.util.concurrent.Future;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;
import infra.web.socket.handler.LoggingWebSocketHandler;
import infra.web.socket.handler.TextWebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;

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
