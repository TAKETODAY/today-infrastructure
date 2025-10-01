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

import java.util.Map;

import infra.web.RequestContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;

/**
 * An interface for WebSocket handlers that support handshake. Can be used to inspect the
 * handshake request and response as well as to pass attributes to the target
 * {@link WebSocketSession}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see WebSocketHandler
 * @see HandshakeInterceptor
 * @since 5.0 2024/10/10 10:08
 */
public interface HandshakeCapable {

  /**
   * Invoked before the handshake is processed.
   * <p>
   * This method will invoke after all {@link
   * HandshakeInterceptor#beforeHandshake(RequestContext, WebSocketHandler, Map)}
   *
   * @param request the current request
   * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
   * session; the provided attributes are copied, the original map is not used.
   * @return whether to proceed with the handshake ({@code true}) or abort ({@code false})
   * @throws Throwable The error will handle by {@link infra.web.HandlerExceptionHandler}
   * @see HandshakeInterceptor#beforeHandshake(RequestContext, WebSocketHandler, Map)
   */
  default boolean beforeHandshake(RequestContext request, Map<String, Object> attributes) throws Throwable {
    return true;
  }

  /**
   * Invoked after the handshake is done. The response status and headers indicate
   * the results of the handshake, i.e. whether it was successful or not.
   * <p>
   * This method will invoke before all {@link
   * HandshakeInterceptor#afterHandshake(RequestContext, WebSocketHandler, Throwable)}
   *
   * @param request the current request
   * @param session websocket session, or {@code null} if handshake failed
   * @param failure an exception raised during the handshake, or {@code null} if none
   * @throws Throwable The error will handle by {@link infra.web.HandlerExceptionHandler}
   * @see HandshakeInterceptor#afterHandshake(RequestContext, WebSocketHandler, Throwable)
   */
  default void afterHandshake(RequestContext request, @Nullable WebSocketSession session, @Nullable Throwable failure)
          throws Throwable {

  }

}
