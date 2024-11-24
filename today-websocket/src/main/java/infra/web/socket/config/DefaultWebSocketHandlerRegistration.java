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

package infra.web.socket.config;

import java.util.Arrays;

import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.web.HttpRequestHandler;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeHandler;
import infra.web.socket.server.HandshakeInterceptor;
import infra.web.socket.server.support.WebSocketHttpRequestHandler;

/**
 * A helper class for configuring {@link WebSocketHandler} request handling
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/22 22:41
 */
public class DefaultWebSocketHandlerRegistration
        extends AbstractWebSocketHandlerRegistration<MultiValueMap<HttpRequestHandler, String>> {

  @Override
  protected MultiValueMap<HttpRequestHandler, String> createMappings() {
    return new LinkedMultiValueMap<>();
  }

  @Override
  protected void addWebSocketHandlerMapping(MultiValueMap<HttpRequestHandler, String> mappings,
          WebSocketHandler webSocketHandler, HandshakeHandler handshakeHandler,
          HandshakeInterceptor[] interceptors, String path) {

    var httpHandler = new WebSocketHttpRequestHandler(webSocketHandler, handshakeHandler);
    if (ObjectUtils.isNotEmpty(interceptors)) {
      httpHandler.setHandshakeInterceptors(Arrays.asList(interceptors));
    }
    mappings.add(httpHandler, path);
  }

}

