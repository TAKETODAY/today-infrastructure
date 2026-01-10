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

import infra.context.Lifecycle;
import infra.context.SmartLifecycle;
import infra.http.HttpMethod;
import infra.web.RequestContext;
import infra.web.handler.HandlerExecutionChain;
import infra.web.handler.SimpleUrlHandlerMapping;

/**
 * Extension of {@link SimpleUrlHandlerMapping} with support for more
 * precise mapping of WebSocket handshake requests to handlers of type
 * {@link WebSocketHttpRequestHandler}. Also delegates {@link Lifecycle}
 * methods to handlers in the {@link #getUrlMap()} that implement it.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebSocketHandlerMapping extends SimpleUrlHandlerMapping implements SmartLifecycle {

  private boolean webSocketUpgradeMatch;

  private volatile boolean running;

  /**
   * When this is set, if the matched handler is
   * {@link WebSocketHttpRequestHandler}, ensure the request is a WebSocket
   * handshake, i.e. HTTP GET with the header {@code "Upgrade:websocket"},
   * or otherwise suppress the match and return {@code null} allowing another
   * {@link infra.web.HandlerMapping} to match for the
   * same URL path.
   *
   * @param match whether to enable matching on {@code "Upgrade: websocket"}
   */
  public void setWebSocketUpgradeMatch(boolean match) {
    this.webSocketUpgradeMatch = match;
  }

  @Override
  public void start() {
    if (!isRunning()) {
      this.running = true;
      for (Object handler : getUrlMap().values()) {
        if (handler instanceof Lifecycle lifecycle) {
          lifecycle.start();
        }
      }
    }
  }

  @Override
  public void stop() {
    if (isRunning()) {
      this.running = false;
      for (Object handler : getUrlMap().values()) {
        if (handler instanceof Lifecycle lifecycle) {
          lifecycle.stop();
        }
      }
    }
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  @Nullable
  protected Object getHandlerInternal(RequestContext request) {
    Object handler = super.getHandlerInternal(request);
    return (matchWebSocketUpgrade(handler, request) ? handler : null);
  }

  private boolean matchWebSocketUpgrade(@Nullable Object handler, RequestContext request) {
    handler = (handler instanceof HandlerExecutionChain ?
               ((HandlerExecutionChain) handler).getRawHandler() : handler);
    if (this.webSocketUpgradeMatch && handler instanceof WebSocketHttpRequestHandler) {
      String header = request.requestHeaders().getUpgrade();
      return request.getMethod() == HttpMethod.GET
              && header != null && header.equalsIgnoreCase("websocket");
    }
    return true;
  }
}
