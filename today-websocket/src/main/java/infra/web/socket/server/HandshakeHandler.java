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

package infra.web.socket.server;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.web.RequestContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.handler.PerConnectionWebSocketHandler;

/**
 * Contract for processing a WebSocket handshake request.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandshakeInterceptor
 * @see infra.web.socket.server.support.WebSocketHttpRequestHandler
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
