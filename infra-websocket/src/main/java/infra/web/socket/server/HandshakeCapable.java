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
