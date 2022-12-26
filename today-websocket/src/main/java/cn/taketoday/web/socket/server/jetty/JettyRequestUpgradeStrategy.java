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

package cn.taketoday.web.socket.server.jetty;

import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.jetty.JettyWebSocketHandler;
import cn.taketoday.web.socket.jetty.JettyWebSocketSession;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A {@link RequestUpgradeStrategy} for Jetty 11.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

  private static final String[] SUPPORTED_VERSIONS = new String[] { "13" };

  @Override
  public String[] getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }

  @Override
  public List<WebSocketExtension> getSupportedExtensions(RequestContext request) {
    return Collections.emptyList();
  }

  @Override
  public void upgrade(RequestContext request, @Nullable String selectedProtocol,
          List<WebSocketExtension> selectedExtensions, WebSocketHandler handler,
          Map<String, Object> attributes) throws HandshakeFailureException {

    HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);

    ServletContext servletContext = servletRequest.getServletContext();
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(request);

    JettyWebSocketSession session = new JettyWebSocketSession(request.getHeaders());

    if (!attributes.isEmpty()) {
      session.getAttributes().putAll(attributes);
    }

    JettyWebSocketHandler handlerAdapter = new JettyWebSocketHandler(handler, session);

    JettyWebSocketCreator webSocketCreator = (upgradeRequest, upgradeResponse) -> {
      if (selectedProtocol != null) {
        upgradeResponse.setAcceptedSubProtocol(selectedProtocol);
      }
      return handlerAdapter;
    };

    JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(servletContext);

    try {
      container.upgrade(webSocketCreator, servletRequest, servletResponse);
    }
    catch (UndeclaredThrowableException ex) {
      throw new HandshakeFailureException("Failed to upgrade", ex.getUndeclaredThrowable());
    }
    catch (Exception ex) {
      throw new HandshakeFailureException("Failed to upgrade", ex);
    }
  }

}
