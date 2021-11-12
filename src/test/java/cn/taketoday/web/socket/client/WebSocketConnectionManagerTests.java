/*
 * Copyright 2002-2019 the original author or authors.
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

package cn.taketoday.web.socket.client;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.util.concurrent.ListenableFutureTask;
import cn.taketoday.web.socket.LoggingWebSocketHandlerDecorator;
import cn.taketoday.web.socket.TextWebSocketHandler;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHandlerDecorator;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.util.UriComponentsBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

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

    WebSocketHandlerDecorator loggingHandler = (WebSocketHandlerDecorator) client.webSocketHandler;
    assertThat(loggingHandler.getClass()).isEqualTo(LoggingWebSocketHandlerDecorator.class);

    assertThat(loggingHandler.getDelegate()).isSameAs(handler);
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
    public ListenableFuture<WebSocketSession> doHandshake(
            WebSocketHandler handler, String uriTemplate, Object... uriVars) {

      URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
      return doHandshake(handler, null, uri);
    }

    @Override
    public ListenableFuture<WebSocketSession> doHandshake(
            WebSocketHandler handler, WebSocketHttpHeaders headers, URI uri) {

      this.webSocketHandler = handler;
      this.headers = headers;
      this.uri = uri;
      return new ListenableFutureTask<>(() -> null);
    }
  }

}
