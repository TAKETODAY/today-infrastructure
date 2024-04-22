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

package cn.taketoday.web.socket.config;

import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.config.WebMvcConfigurationSupport;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;

/**
 * Configuration support for WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebSocketConfigurationSupport {

  @Component
  public WebSocketHandlerMapping webSocketHandlerMapping(
          @Nullable WebMvcConfigurationSupport support, @Nullable RequestUpgradeStrategy requestUpgradeStrategy) {
    DefaultWebSocketHandlerRegistry registry = new DefaultWebSocketHandlerRegistry();
    if (requestUpgradeStrategy != null) {
      registry.setHandshakeHandler(new DefaultHandshakeHandler(requestUpgradeStrategy));
    }

    registerWebSocketHandlers(registry);
    WebSocketHandlerMapping handlerMapping = registry.getHandlerMapping();
    if (support != null) {
      support.initHandlerMapping(handlerMapping);
    }
    return handlerMapping;
  }

  protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

  }

}
