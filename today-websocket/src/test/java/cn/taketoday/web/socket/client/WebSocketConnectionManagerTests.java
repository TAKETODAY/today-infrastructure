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

package cn.taketoday.web.socket.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.handler.LoggingWebSocketHandlerDecorator;
import cn.taketoday.web.socket.handler.TextWebSocketHandler;
import cn.taketoday.web.util.UriComponentsBuilder;

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
    assertThat(loggingHandler.getClass()).isEqualTo(LoggingWebSocketHandlerDecorator.class);

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
    public Future<WebSocketSession> connect(
            WebSocketHandler handler, String uriTemplate, Object... uriVars) {

      URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
      return connect(handler, null, uri);
    }

    @Override
    public Future<WebSocketSession> connect(
            WebSocketHandler handler, WebSocketHttpHeaders headers, URI uri) {

      this.webSocketHandler = handler;
      this.headers = headers;
      this.uri = uri;
      return Future.forFutureTask(() -> null);
    }


  }

}
