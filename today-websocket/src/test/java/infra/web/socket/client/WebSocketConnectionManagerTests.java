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
