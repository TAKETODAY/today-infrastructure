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

package infra.web.socket.server;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import infra.web.RequestContext;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;

/**
 * A server-specific strategy for performing the actual upgrade to a WebSocket exchange.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/20 22:23
 */
public interface RequestUpgradeStrategy {

  /**
   * Return the supported WebSocket protocol versions.
   */
  String[] getSupportedVersions();

  /**
   * Return the WebSocket protocol extensions supported by the underlying WebSocket server.
   */
  List<WebSocketExtension> getSupportedExtensions(RequestContext request);

  /**
   * Perform runtime specific steps to complete the upgrade. Invoked after successful
   * negotiation of the handshake request.
   *
   * @param request the current request
   * @param selectedProtocol the selected sub-protocol, if any
   * @param selectedExtensions the selected WebSocket protocol extensions
   * @param wsHandler the handler for WebSocket messages
   * @param attributes handshake request specific attributes to be set on the WebSocket
   * session via {@link infra.web.socket.server.HandshakeInterceptor} and
   * thus made available to the {@link infra.web.socket.WebSocketHandler}
   * @throws HandshakeFailureException thrown when handshake processing failed to
   * complete due to an internal, unrecoverable error, i.e. a server error as
   * opposed to a failure to successfully negotiate the requirements of the
   * handshake request.
   */
  @Nullable
  WebSocketSession upgrade(RequestContext request, @Nullable String selectedProtocol,
          List<WebSocketExtension> selectedExtensions, WebSocketHandler wsHandler,
          Map<String, Object> attributes) throws HandshakeFailureException;

}
