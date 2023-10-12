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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.Decorator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for the Jakarta WebSocket API 2.1+.
 *
 * <p>This strategy serves as a fallback if no specific server has been detected.
 * It can also be used with Jakarta EE 10 level servers such as Tomcat 10.1 and
 * Undertow 2.3 directly, relying on their built-in Jakarta WebSocket 2.1 support.
 *
 * <p>To modify properties of the underlying {@link jakarta.websocket.server.ServerContainer}
 * you can use {@link ServletServerContainerFactoryBean} in XML configuration or,
 * when using Java configuration, access the container instance through the
 * "jakarta.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see jakarta.websocket.server.ServerContainer#upgradeHttpToWebSocket
 * @since 4.0
 */
public class StandardWebSocketUpgradeStrategy extends AbstractStandardUpgradeStrategy {

  private static final String[] SUPPORTED_VERSIONS = new String[] { "13" };

  public StandardWebSocketUpgradeStrategy() {
    this(null);
  }

  public StandardWebSocketUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    super(sessionDecorator);
  }

  @Override
  public String[] getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }

  @Override
  protected void upgradeInternal(RequestContext request, @Nullable String selectedProtocol,
          List<Extension> selectedExtensions, Endpoint endpoint) throws HandshakeFailureException {

    HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(request);
    String path = request.getRequestURI();  // shouldn't matter
    Map<String, String> pathParams = Collections.emptyMap();

    ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
    endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
    endpointConfig.setExtensions(selectedExtensions);

    try {
      upgradeHttpToWebSocket(servletRequest, servletResponse, endpointConfig, pathParams);
    }
    catch (Exception ex) {
      throw new HandshakeFailureException(
              "Servlet request failed to upgrade to WebSocket: " + request.getRequestURL(), ex);
    }
  }

  protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
          ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

    getContainer(request).upgradeHttpToWebSocket(request, response, endpointConfig, pathParams);
  }

}
