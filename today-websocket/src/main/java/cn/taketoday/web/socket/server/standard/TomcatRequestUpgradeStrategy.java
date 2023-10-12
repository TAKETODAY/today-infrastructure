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

package cn.taketoday.web.socket.server.standard;

import org.apache.tomcat.websocket.server.WsServerContainer;

import java.util.Map;

import cn.taketoday.core.Decorator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.WebSocketSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Apache Tomcat. Compatible with Tomcat 10
 * and higher, in particular with Tomcat 10.0 (not based on Jakarta WebSocket 2.1 yet).
 *
 * <p>To modify properties of the underlying {@link jakarta.websocket.server.ServerContainer}
 * you can use {@link ServletServerContainerFactoryBean} in XML configuration or,
 * when using Java configuration, access the container instance through the
 * "jakarta.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WsServerContainer#upgradeHttpToWebSocket
 * @since 4.0
 */
public class TomcatRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

  public TomcatRequestUpgradeStrategy() {
    super(null);
  }

  public TomcatRequestUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    super(sessionDecorator);
  }

  @Override
  protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
          ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

    getContainer(request).upgradeHttpToWebSocket(
            request, response, endpointConfig, pathParams);
  }

}
