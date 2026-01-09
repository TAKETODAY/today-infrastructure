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

import java.net.URI;

import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.util.concurrent.Future;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.util.UriBuilder;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

/**
 * Contract for initiating a WebSocket request. As an alternative considering using the
 * declarative style {@link WebSocketConnectionManager} that starts a WebSocket connection
 * to a pre-configured URI when the application starts.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebSocketConnectionManager
 * @since 4.0 2021/11/12 15:58
 */
public interface WebSocketClient {

  /**
   * Execute a handshake request to the given url and handle the resulting
   * WebSocket session with the given handler.
   *
   * @param handler the session handler
   * @param uri the URI
   * @return a future that completes when the session is available
   * @since 5.0
   */
  default Future<WebSocketSession> connect(UriBuilder uri, @Nullable HttpHeaders headers, WebSocketHandler handler) {
    return connect(uri.build(), headers, handler);
  }

  /**
   * Execute a handshake request to the given url and handle the resulting
   * WebSocket session with the given handler.
   *
   * @param handler the session handler
   * @param uri the URI
   * @return a future that completes when the session is available
   * @since 5.0
   */
  default Future<WebSocketSession> connect(UriComponents uri, @Nullable HttpHeaders headers, WebSocketHandler handler) {
    return connect(uri.toURI(), headers, handler);
  }

  /**
   * Execute a handshake request to the given url and handle the resulting
   * WebSocket session with the given handler.
   *
   * @param handler the websocket handler
   * @param uriTemplate the url template
   * @param uriVariables the variables to expand the template
   * @return a future that completes when the session is available
   */
  default Future<WebSocketSession> connect(WebSocketHandler handler, String uriTemplate, Object... uriVariables) {
    Assert.notNull(uriTemplate, "'uriTemplate' is required");
    URI uri = UriComponentsBuilder.forURIString(uriTemplate).buildAndExpand(uriVariables).encode().toURI();
    return connect(uri, null, handler);
  }

  /**
   * Execute a handshake request to the given url and handle the resulting
   * WebSocket session with the given handler.
   *
   * @param handler the session handler
   * @param uri the URI
   * @return a future that completes when the session is available
   * @since 5.0
   */
  Future<WebSocketSession> connect(URI uri, @Nullable HttpHeaders headers, WebSocketHandler handler);

}
