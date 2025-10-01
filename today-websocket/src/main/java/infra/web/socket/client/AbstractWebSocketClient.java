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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.concurrent.Future;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;

/**
 * Abstract base class for {@link WebSocketClient} implementations.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

  private static final Set<String> specialHeaders = Set.of(
          "cache-control", "connection", "host", "sec-websocket-extensions", "sec-websocket-key",
          "sec-websocket-protocol", "sec-websocket-version", "pragma", "upgrade");

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public Future<WebSocketSession> connect(URI uri, @Nullable HttpHeaders headers, WebSocketHandler handler) {
    Assert.notNull(handler, "WebSocketHandler is required");
    assertURI(uri);

    if (logger.isDebugEnabled()) {
      logger.debug("Connecting to {}", uri);
    }

    List<String> subProtocols = Collections.emptyList();
    List<WebSocketExtension> extensions = Collections.emptyList();

    HttpHeaders headersToUse = HttpHeaders.forWritable();
    if (headers != null) {
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String header = entry.getKey();
        List<String> values = entry.getValue();
        if (values != null && !specialHeaders.contains(header.toLowerCase(Locale.ROOT))) {
          headersToUse.put(header, values);
        }
      }

      WebSocketHttpHeaders wsth;
      if (headers instanceof WebSocketHttpHeaders) {
        wsth = (WebSocketHttpHeaders) headers;
      }
      else {
        wsth = new WebSocketHttpHeaders(headers);
      }
      subProtocols = wsth.getSecWebSocketProtocol();
      extensions = wsth.getSecWebSocketExtensions();
    }

    return doHandshakeInternal(handler, headersToUse, uri, subProtocols, extensions);
  }

  protected void assertURI(URI uri) {
    Assert.notNull(uri, "URI is required");
    String scheme = uri.getScheme();
    if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
      throw new IllegalArgumentException("Invalid scheme: " + scheme);
    }
  }

  /**
   * Perform the actual handshake to establish a connection to the server.
   *
   * @param webSocketHandler the client-side handler for WebSocket messages
   * @param headers the HTTP headers to use for the handshake, with unwanted (forbidden)
   * headers filtered out (never {@code null})
   * @param uri the target URI for the handshake (never {@code null})
   * @param subProtocols requested sub-protocols, or an empty list
   * @param extensions requested WebSocket extensions, or an empty list
   * @return the established WebSocket session wrapped in a ListenableFuture.
   */
  protected abstract Future<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
          HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions);

}
