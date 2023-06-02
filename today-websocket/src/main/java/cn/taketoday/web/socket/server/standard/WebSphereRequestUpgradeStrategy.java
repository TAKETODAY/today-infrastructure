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

package cn.taketoday.web.socket.server.standard;

import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSphere support for upgrading an {@link HttpServletRequest} during a
 * WebSocket handshake. To modify properties of the underlying
 * {@link ServerContainer} you can use
 * {@link ServletServerContainerFactoryBean} in XML configuration or, when using
 * Java configuration, access the container instance through the
 * "jakarta.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class WebSphereRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

  private static final Method upgradeMethod;

  static {
    ClassLoader loader = WebSphereRequestUpgradeStrategy.class.getClassLoader();
    try {
      Class<?> type = loader.loadClass("com.ibm.websphere.wsoc.WsWsocServerContainer");
      upgradeMethod = type.getMethod("doUpgrade", HttpServletRequest.class,
              HttpServletResponse.class, ServerEndpointConfig.class, Map.class);
    }
    catch (Exception ex) {
      throw new IllegalStateException("No compatible WebSphere version found", ex);
    }
  }

  @Override
  protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
          ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

    ServerContainer container = getContainer(request);
    upgradeMethod.invoke(container, request, response, endpointConfig, pathParams);
  }

}
