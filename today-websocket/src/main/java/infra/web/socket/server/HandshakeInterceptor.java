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

/**
 * Interceptor for WebSocket handshake requests. Can be used to inspect the
 * handshake request and response as well as to pass attributes to the target
 * {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.web.socket.server.support.WebSocketHttpRequestHandler
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
   * @throws Exception The error will handle by {@link infra.web.HandlerExceptionHandler}
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
  void afterHandshake(RequestContext request, WebSocketHandler wsHandler, @Nullable Throwable exception);

}
