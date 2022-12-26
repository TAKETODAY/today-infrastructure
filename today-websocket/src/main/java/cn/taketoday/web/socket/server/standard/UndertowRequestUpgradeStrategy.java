/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.Map;

import io.undertow.websockets.jsr.ServerWebSocketContainer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for WildFly and its underlying
 * Undertow web server. Also compatible with embedded Undertow usage.
 *
 * <p>Designed for Undertow 2.2, also compatible with Undertow 2.3
 * (which implements Jakarta WebSocket 2.1 as well).
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see ServerWebSocketContainer#doUpgrade
 * @since 4.0
 */
public class UndertowRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

  private static final String[] SUPPORTED_VERSIONS = new String[] { "13", "8", "7" };

  @Override
  public String[] getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }

  @Override
  protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
          ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

    ((ServerWebSocketContainer) getContainer(request)).doUpgrade(
            request, response, endpointConfig, pathParams);
  }

}
