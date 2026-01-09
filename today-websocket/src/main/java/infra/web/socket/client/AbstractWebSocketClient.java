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
