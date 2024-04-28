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

/**
 * Interceptor for WebSocket handshake requests. Can be used to inspect the
 * handshake request and response as well as to pass attributes to the target
 * {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.socket.server.support.WebSocketHttpRequestHandler
 * @since 4.0
 */
public interface HandshakeInterceptor {

  /**
   * Invoked before the handshake is processed.
   *
   * @param request the current request
   * @param wsHandler the target WebSocket handler
   * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
   * session; the provided attributes are copied, the original map is not used.
   * @return whether to proceed with the handshake ({@code true}) or abort ({@code false})
   */
  boolean beforeHandshake(RequestContext request, WebSocketHandler wsHandler, Map<String, Object> attributes)
          throws Exception;

  /**
   * Invoked after the handshake is done. The response status and headers indicate
   * the results of the handshake, i.e. whether it was successful or not.
   *
   * @param request the current request
   * @param wsHandler the target WebSocket handler
   * @param exception an exception raised during the handshake, or {@code null} if none
   */
  void afterHandshake(RequestContext request, WebSocketHandler wsHandler, @Nullable Exception exception);

}
