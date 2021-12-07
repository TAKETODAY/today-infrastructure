/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * Abstract base class for {@link WebSocketClient} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

  private static final HashSet<String> specialHeaders = new HashSet<>();

  static {
    specialHeaders.add("cache-control");
    specialHeaders.add("connection");
    specialHeaders.add("host");
    specialHeaders.add("sec-websocket-extensions");
    specialHeaders.add("sec-websocket-key");
    specialHeaders.add("sec-websocket-protocol");
    specialHeaders.add("sec-websocket-version");
    specialHeaders.add("pragma");
    specialHeaders.add("upgrade");
  }

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public ListenableFuture<WebSocketSession> doHandshake(
          WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVars) {

    Assert.notNull(uriTemplate, "'uriTemplate' must not be null");
    URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
    return doHandshake(webSocketHandler, null, uri);
  }

  @Override
  public final ListenableFuture<WebSocketSession> doHandshake(
          WebSocketHandler webSocketHandler, @Nullable WebSocketHttpHeaders headers, URI uri) {

    Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
    assertUri(uri);

    if (logger.isDebugEnabled()) {
      logger.debug("Connecting to " + uri);
    }

    HttpHeaders headersToUse = HttpHeaders.create();
    if (headers != null) {
      headers.forEach((header, values) -> {
        if (values != null && !specialHeaders.contains(header.toLowerCase())) {
          headersToUse.put(header, values);
        }
      });
    }

    List<String> subProtocols =
            (headers != null ? headers.getSecWebSocketProtocol() : Collections.emptyList());
    List<WebSocketExtension> extensions =
            (headers != null ? headers.getSecWebSocketExtensions() : Collections.emptyList());

    return doHandshakeInternal(webSocketHandler, headersToUse, uri, subProtocols, extensions);
  }

  protected void assertUri(URI uri) {
    Assert.notNull(uri, "URI must not be null");
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
  protected abstract ListenableFuture<WebSocketSession> doHandshakeInternal(
          WebSocketHandler webSocketHandler,
          HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions);

}
