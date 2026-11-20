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

package infra.web.socket.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import infra.http.HttpHeaders;
import infra.util.concurrent.Future;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AbstractWebSocketClientTests {

  @Test
  void filtersSpecialHeadersAndExtractsWebSocketHeaders() {
    TestClient client = new TestClient();
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.set("Authorization", "Bearer token");
    headers.set("Connection", "Upgrade");
    headers.setSecWebSocketProtocol(List.of("chat"));
    headers.setSecWebSocketExtensions(List.of(new WebSocketExtension("permessage-deflate")));

    client.connect(URI.create("ws://localhost:8080/ws"), headers, mock(WebSocketHandler.class));

    assertThat(client.headers.get("Authorization")).containsExactly("Bearer token");
    assertThat(client.headers.get("Connection")).isNull();
    assertThat(client.protocols).containsExactly("chat");
    assertThat(client.extensions).containsExactly(new WebSocketExtension("permessage-deflate"));
  }

  @Test
  void acceptsPlainHeadersAndValidatesArguments() {
    TestClient client = new TestClient();
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Test", "value");
    client.connect(URI.create("wss://localhost/ws"), headers, mock(WebSocketHandler.class));
    assertThat(client.headers.get("X-Test")).containsExactly("value");

    assertThatThrownBy(() -> client.connect(URI.create("http://localhost/ws"), null, mock(WebSocketHandler.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid scheme: http");
    assertThatThrownBy(() -> client.connect((URI) null, null, mock(WebSocketHandler.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("URI is required");
    assertThatThrownBy(() -> client.connect(URI.create("ws://localhost/ws"), null, null))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("WebSocketHandler is required");
  }

  @Test
  void connectsUsingUriBuilderAndComponents() {
    TestClient client = new TestClient();
    WebSocketHandler handler = mock(WebSocketHandler.class);
    var builder = UriComponentsBuilder.forURIString("ws://example.test/socket");
    client.connect(builder, null, handler);
    assertThat(client.uri).isEqualTo(URI.create("ws://example.test/socket"));
    assertThat(client.protocols).isEmpty();

    client.connect(builder.path("/next").build(), null, handler);
    assertThat(client.uri).isEqualTo(URI.create("ws://example.test/socket/next"));
    assertThat(client.extensions).isEmpty();
  }

  private static final class TestClient extends AbstractWebSocketClient {

    private HttpHeaders headers;

    private List<String> protocols;

    private List<WebSocketExtension> extensions;

    private URI uri;

    @Override
    protected Future<WebSocketSession> doHandshakeInternal(WebSocketHandler handler, HttpHeaders headers,
            URI uri, List<String> subProtocols, List<WebSocketExtension> extensions) {
      this.headers = headers;
      this.protocols = subProtocols;
      this.extensions = extensions;
      this.uri = uri;
      return Future.ok();
    }
  }

}
