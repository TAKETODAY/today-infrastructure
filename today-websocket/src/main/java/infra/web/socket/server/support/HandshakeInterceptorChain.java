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

package infra.web.socket.server.support;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.RequestContext;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.HandshakeCapable;
import infra.web.socket.server.HandshakeInterceptor;

/**
 * A helper class that assists with invoking a list of handshake interceptors.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HandshakeInterceptorChain {

  private static final Logger logger = LoggerFactory.getLogger(HandshakeInterceptorChain.class);

  private final List<HandshakeInterceptor> interceptors;

  private final WebSocketHandler wsHandler;

  private int interceptorIndex = -1;

  public HandshakeInterceptorChain(@Nullable List<HandshakeInterceptor> interceptors, WebSocketHandler wsHandler) {
    this.interceptors = (interceptors != null ? interceptors : Collections.emptyList());
    this.wsHandler = wsHandler;
  }

  public boolean applyBeforeHandshake(RequestContext request, Map<String, Object> attributes) throws Throwable {
    for (int i = 0; i < this.interceptors.size(); i++) {
      HandshakeInterceptor interceptor = interceptors.get(i);
      if (!interceptor.beforeHandshake(request, this.wsHandler, attributes)) {
        if (logger.isDebugEnabled()) {
          logger.debug("{} returns false from beforeHandshake - precluding handshake", interceptor);
        }
        applyAfterHandshake(request, null, null);
        return false;
      }
      this.interceptorIndex = i;
    }

    if (wsHandler instanceof HandshakeCapable hc) {
      if (!hc.beforeHandshake(request, attributes)) {
        if (logger.isDebugEnabled()) {
          logger.debug("{} returns false from beforeHandshake - precluding handshake", wsHandler);
        }
        applyAfterHandshake(request, null, null);
        return false;
      }
    }

    return true;
  }

  public void applyAfterHandshake(RequestContext request, @Nullable WebSocketSession session, @Nullable Throwable failure) throws Throwable {
    if (wsHandler instanceof HandshakeCapable hc) {
      hc.afterHandshake(request, session, failure);
    }

    for (int i = this.interceptorIndex; i >= 0; i--) {
      HandshakeInterceptor interceptor = this.interceptors.get(i);
      try {
        interceptor.afterHandshake(request, this.wsHandler, failure);
      }
      catch (Exception ex) {
        if (logger.isWarnEnabled()) {
          logger.warn("{} threw exception in afterHandshake: {}", interceptor, ex.toString());
        }
      }
    }
  }

}
