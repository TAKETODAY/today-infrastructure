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

