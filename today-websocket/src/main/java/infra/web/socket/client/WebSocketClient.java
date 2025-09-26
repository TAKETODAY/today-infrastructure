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
