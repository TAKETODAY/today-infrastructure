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

package cn.taketoday.web.socket.server;

import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.handler.PerConnectionWebSocketHandler;

/**
 * Contract for processing a WebSocket handshake request.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandshakeInterceptor
 * @see cn.taketoday.web.socket.server.support.WebSocketHttpRequestHandler
 * @since 4.0
 */
public interface HandshakeHandler {

  /**
   * Initiate the handshake.
   *
   * @param request the current request
   * @param wsHandler the handler to process WebSocket messages; see
   * {@link PerConnectionWebSocketHandler} for providing a handler with
   * per-connection lifecycle.
   * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
   * session; the provided attributes are copied, the original map is not used.
   * @return the handshake websocket session. In either case the
   * response status, headers, and body will have been updated to reflect the
   * result of the negotiation
   * @throws HandshakeFailureException thrown when handshake processing failed to
   * complete due to an internal, unrecoverable error, i.e. a server error as
   * opposed to a failure to successfully negotiate the handshake.
   */
  @Nullable
  WebSocketSession doHandshake(RequestContext request, WebSocketHandler wsHandler, Map<String, Object> attributes)
          throws HandshakeFailureException;

}
