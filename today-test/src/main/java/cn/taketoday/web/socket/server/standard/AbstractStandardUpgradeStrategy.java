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

package cn.taketoday.web.socket.server.standard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.Decorator;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.ServletUtils;
import cn.taketoday.web.socket.StandardEndpoint;
import cn.taketoday.web.socket.StandardWebSocketSession;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.http.HttpMockRequest;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerContainer;

/**
 * A base class for {@link RequestUpgradeStrategy} implementations that build
 * on the standard WebSocket API for Java (JSR-356).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractStandardUpgradeStrategy implements RequestUpgradeStrategy {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private volatile List<WebSocketExtension> extensions;

  @Nullable
  private final Decorator<WebSocketSession> sessionDecorator;

  protected AbstractStandardUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    this.sessionDecorator = sessionDecorator;
  }

  protected ServerContainer getContainer(HttpMockRequest request) {
    MockContext mockContext = request.getServletContext();
    String attrName = "jakarta.websocket.server.ServerContainer";
    ServerContainer container = (ServerContainer) mockContext.getAttribute(attrName);
    Assert.notNull(container, "No 'jakarta.websocket.server.ServerContainer' ServletContext attribute. " +
            "Are you running in a Servlet container that supports JSR-356?");
    return container;
  }

  @Override
  public List<WebSocketExtension> getSupportedExtensions(RequestContext request) {
    List<WebSocketExtension> extensions = this.extensions;
    if (extensions == null) {
      HttpMockRequest servletRequest = ServletUtils.getServletRequest(request);
      extensions = getInstalledExtensions(getContainer(servletRequest));
      this.extensions = extensions;
    }
    return extensions;
  }

  protected List<WebSocketExtension> getInstalledExtensions(WebSocketContainer container) {
    ArrayList<WebSocketExtension> result = new ArrayList<>();
    for (Extension extension : container.getInstalledExtensions()) {
      result.add(new StandardToWebSocketExtensionAdapter(extension));
    }
    return result;
  }

  @Override
  public WebSocketSession upgrade(RequestContext request, @Nullable String selectedProtocol,
          List<WebSocketExtension> selectedExtensions, WebSocketHandler wsHandler,
          Map<String, Object> attrs) throws HandshakeFailureException {

    HttpHeaders headers = request.getHeaders();

    WebSocketSession session = createSession(headers);
    if (sessionDecorator != null) {
      session = sessionDecorator.decorate(session);
    }

    if (!attrs.isEmpty()) {
      session.getAttributes().putAll(attrs);
    }
    StandardEndpoint endpoint = new StandardEndpoint(session, wsHandler);

    ArrayList<Extension> extensions = new ArrayList<>();
    for (WebSocketExtension extension : selectedExtensions) {
      extensions.add(new WebSocketToStandardExtensionAdapter(extension));
    }

    upgradeInternal(request, selectedProtocol, extensions, endpoint);
    return session;
  }

  protected WebSocketSession createSession(HttpHeaders headers) {
    return new StandardWebSocketSession(headers, null, null);
  }

  protected abstract void upgradeInternal(RequestContext request, @Nullable String selectedProtocol,
          List<Extension> selectedExtensions, Endpoint endpoint) throws HandshakeFailureException;

}
