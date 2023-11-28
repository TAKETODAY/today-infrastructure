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

package cn.taketoday.web.socket.server.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.HandshakeInterceptor;

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

  public boolean applyBeforeHandshake(RequestContext request, Map<String, Object> attributes) throws Exception {
    for (int i = 0; i < this.interceptors.size(); i++) {
      HandshakeInterceptor interceptor = interceptors.get(i);
      if (!interceptor.beforeHandshake(request, this.wsHandler, attributes)) {
        if (logger.isDebugEnabled()) {
          logger.debug("{} returns false from beforeHandshake - precluding handshake", interceptor);
        }
        applyAfterHandshake(request, null);
        return false;
      }
      this.interceptorIndex = i;
    }
    return true;
  }

  public void applyAfterHandshake(RequestContext request, @Nullable Exception failure) {
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
