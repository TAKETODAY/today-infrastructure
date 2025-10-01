/*
 * Copyright 2017 - 2025 the original author or authors.
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
