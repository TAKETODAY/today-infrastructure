/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.context.Lifecycle;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.SimpleUrlHandlerMapping;

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
   * {@link cn.taketoday.web.HandlerMapping} to match for the
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
