/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.HandshakeHandler;
import cn.taketoday.web.socket.server.HandshakeInterceptor;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;
import cn.taketoday.web.socket.server.support.OriginHandshakeInterceptor;

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
    Assert.notNull(handler, "WebSocketHandler must not be null");
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
