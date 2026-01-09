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

package infra.web.socket.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.util.MultiValueMap;
import infra.web.HttpRequestHandler;
import infra.web.handler.SimpleUrlHandlerMapping;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeHandler;
import infra.web.socket.server.support.WebSocketHandlerMapping;

/**
 * {@link WebSocketHandlerRegistry} with MVC handler mappings for the
 * handshake requests.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultWebSocketHandlerRegistry implements WebSocketHandlerRegistry {

  private final List<DefaultWebSocketHandlerRegistration> registrations = new ArrayList<>(4);

  private int order = 1;

  @Nullable
  private HandshakeHandler handshakeHandler;

  public DefaultWebSocketHandlerRegistry() {

  }

  @Override
  public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
    DefaultWebSocketHandlerRegistration registration = new DefaultWebSocketHandlerRegistration();
    registration.addHandler(handler, paths);
    registration.setHandshakeHandler(handshakeHandler);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Set the order for the resulting {@link SimpleUrlHandlerMapping} relative to
   * other handler mappings configured in Web MVC.
   * <p>The default value is 1.
   */
  public void setOrder(int order) {
    this.order = order;
  }

  public int getOrder() {
    return this.order;
  }

  public void setHandshakeHandler(@Nullable HandshakeHandler handshakeHandler) {
    this.handshakeHandler = handshakeHandler;
  }

  public WebSocketHandlerMapping getHandlerMapping() {
    LinkedHashMap<String, Object> urlMap = new LinkedHashMap<>();
    for (DefaultWebSocketHandlerRegistration registration : this.registrations) {
      MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
      for (Map.Entry<HttpRequestHandler, List<String>> entry : mappings.entrySet()) {
        for (String pattern : entry.getValue()) {
          urlMap.put(pattern, entry.getKey());
        }
      }
    }

    WebSocketHandlerMapping hm = new WebSocketHandlerMapping();
    hm.setUrlMap(urlMap);
    hm.setOrder(this.order);
    return hm;
  }

}
