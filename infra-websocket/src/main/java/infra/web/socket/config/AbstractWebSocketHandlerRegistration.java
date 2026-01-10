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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import infra.lang.Assert;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeHandler;
import infra.web.socket.server.HandshakeInterceptor;
import infra.web.socket.server.support.DefaultHandshakeHandler;
import infra.web.socket.server.support.OriginHandshakeInterceptor;

/**
 * Base class for {@link WebSocketHandlerRegistration WebSocketHandlerRegistrations}
 * that gathers all the configuration options but allows subclasses to put
 * together the actual HTTP request mappings.
 *
 * @param <M> the mappings type
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/22 22:39
 */
public abstract class AbstractWebSocketHandlerRegistration<M> implements WebSocketHandlerRegistration {

  private final MultiValueMap<WebSocketHandler, String> handlerMap = new LinkedMultiValueMap<>();

  @Nullable
  private HandshakeHandler handshakeHandler;

  private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

  private final List<String> allowedOrigins = new ArrayList<>();

  private final List<String> allowedOriginPatterns = new ArrayList<>();

  @Override
  public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
    Assert.notNull(handler, "WebSocketHandler is required");
    Assert.notEmpty(paths, "Paths must not be empty");
    this.handlerMap.put(handler, Arrays.asList(paths));
    return this;
  }

  @Override
  public WebSocketHandlerRegistration setHandshakeHandler(@Nullable HandshakeHandler handshakeHandler) {
    this.handshakeHandler = handshakeHandler;
    return this;
  }

  @Nullable
  protected HandshakeHandler getHandshakeHandler() {
    return this.handshakeHandler;
  }

  @Override
  public WebSocketHandlerRegistration addInterceptors(HandshakeInterceptor... interceptors) {
    if (ObjectUtils.isNotEmpty(interceptors)) {
      this.interceptors.addAll(Arrays.asList(interceptors));
    }
    return this;
  }

  @Override
  public WebSocketHandlerRegistration setAllowedOrigins(String... allowedOrigins) {
    this.allowedOrigins.clear();
    if (ObjectUtils.isNotEmpty(allowedOrigins)) {
      this.allowedOrigins.addAll(Arrays.asList(allowedOrigins));
    }
    return this;
  }

  @Override
  public WebSocketHandlerRegistration setAllowedOriginPatterns(String... allowedOriginPatterns) {
    this.allowedOriginPatterns.clear();
    if (ObjectUtils.isNotEmpty(allowedOriginPatterns)) {
      this.allowedOriginPatterns.addAll(Arrays.asList(allowedOriginPatterns));
    }
    return this;
  }

  protected HandshakeInterceptor[] getInterceptors() {
    List<HandshakeInterceptor> interceptors = new ArrayList<>(this.interceptors.size() + 1);
    interceptors.addAll(this.interceptors);
    OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowedOrigins);
    if (ObjectUtils.isNotEmpty(this.allowedOriginPatterns)) {
      interceptor.setAllowedOriginPatterns(this.allowedOriginPatterns);
    }
    interceptors.add(interceptor);
    return interceptors.toArray(new HandshakeInterceptor[0]);
  }

  protected final M getMappings() {
    M mappings = createMappings();
    HandshakeHandler handshakeHandler = getOrCreateHandshakeHandler();
    HandshakeInterceptor[] interceptors = getInterceptors();

    for (Map.Entry<WebSocketHandler, List<String>> entry : handlerMap.entrySet()) {
      List<String> paths = entry.getValue();
      WebSocketHandler wsHandler = entry.getKey();
      for (String path : paths) {
        addWebSocketHandlerMapping(mappings, wsHandler, handshakeHandler, interceptors, path);
      }
    }

    return mappings;
  }

  private HandshakeHandler getOrCreateHandshakeHandler() {
    return handshakeHandler != null ? handshakeHandler : new DefaultHandshakeHandler();
  }

  protected abstract M createMappings();

  protected abstract void addWebSocketHandlerMapping(M mappings, WebSocketHandler wsHandler,
          HandshakeHandler handshakeHandler, HandshakeInterceptor[] interceptors, String path);

}
