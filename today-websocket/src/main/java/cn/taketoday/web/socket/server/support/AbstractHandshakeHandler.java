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

package cn.taketoday.web.socket.server.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.SubProtocolCapable;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHttpHeaders;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import cn.taketoday.web.socket.server.HandshakeHandler;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import cn.taketoday.web.socket.server.standard.StandardWebSocketUpgradeStrategy;
import cn.taketoday.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import cn.taketoday.web.socket.server.standard.UndertowRequestUpgradeStrategy;

import static cn.taketoday.util.ClassUtils.isPresent;

/**
 * A base class for {@link HandshakeHandler} implementations.
 *
 * <p>Performs initial validation of the WebSocket handshake request - possibly rejecting it
 * through the appropriate HTTP status code - while also allowing its subclasses to override
 * various parts of the negotiation process (e.g. origin validation, sub-protocol negotiation,
 * extensions negotiation, etc).
 *
 * <p>If the negotiation succeeds, the actual upgrade is delegated to a server-specific
 * {@link cn.taketoday.web.socket.server.RequestUpgradeStrategy}, which will update
 * the response as necessary and initialize the WebSocket. Currently, supported servers are
 * Jetty 9.0-9.3, Tomcat 7.0.47+ and 8.x, Undertow 1.0-1.3, GlassFish 4.1+, WebLogic 12.1.3+.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JettyRequestUpgradeStrategy
 * @see TomcatRequestUpgradeStrategy
 * @see UndertowRequestUpgradeStrategy
 * @since 4.0
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler {

  private static final boolean tomcatWsPresent = isPresent(
          "org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", AbstractHandshakeHandler.class);

  private static final boolean jettyWsPresent = isPresent(
          "org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer", AbstractHandshakeHandler.class);

  private static final boolean undertowWsPresent = isPresent(
          "io.undertow.websockets.jsr.ServerWebSocketContainer", AbstractHandshakeHandler.class);

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final RequestUpgradeStrategy requestUpgradeStrategy;

  private final ArrayList<String> supportedProtocols = new ArrayList<>();

  /**
   * Default constructor that auto-detects and instantiates a
   * {@link RequestUpgradeStrategy} suitable for the runtime container.
   *
   * @throws IllegalStateException if no {@link RequestUpgradeStrategy} can be found.
   */
  protected AbstractHandshakeHandler() {
    this(initRequestUpgradeStrategy());
  }

  /**
   * A constructor that accepts a runtime-specific {@link RequestUpgradeStrategy}.
   *
   * @param requestUpgradeStrategy the upgrade strategy to use
   */
  protected AbstractHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
    Assert.notNull(requestUpgradeStrategy, "RequestUpgradeStrategy is required");
    this.requestUpgradeStrategy = requestUpgradeStrategy;
  }

  /**
   * Return the {@link RequestUpgradeStrategy} for WebSocket requests.
   */
  public RequestUpgradeStrategy getRequestUpgradeStrategy() {
    return this.requestUpgradeStrategy;
  }

  /**
   * Use this property to configure the list of supported sub-protocols.
   * The first configured sub-protocol that matches a client-requested sub-protocol
   * is accepted. If there are no matches the response will not contain a
   * {@literal Sec-WebSocket-Protocol} header.
   * <p>Note that if the WebSocketHandler passed in at runtime is an instance of
   * {@link SubProtocolCapable} then there is no need to explicitly configure
   * this property. That is certainly the case with the built-in STOMP over
   * WebSocket support. Therefore, this property should be configured explicitly
   * only if the WebSocketHandler does not implement {@code SubProtocolCapable}.
   */
  public void setSupportedProtocols(String... protocols) {
    this.supportedProtocols.clear();
    for (String protocol : protocols) {
      this.supportedProtocols.add(protocol.toLowerCase());
    }
  }

  /**
   * Return the list of supported sub-protocols.
   */
  public String[] getSupportedProtocols() {
    return StringUtils.toStringArray(this.supportedProtocols);
  }

  @Override
  public final WebSocketSession doHandshake(RequestContext request, WebSocketHandler wsHandler, Map<String, Object> attributes)
          throws HandshakeFailureException {

    WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
    if (logger.isTraceEnabled()) {
      logger.trace("Processing request {} with headers={}", request.getURI(), headers);
    }
    try {
      if (HttpMethod.GET != request.getMethod()) {
        request.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        request.responseHeaders().setAllow(Collections.singleton(HttpMethod.GET));
        if (logger.isErrorEnabled()) {
          logger.error("Handshake failed due to unexpected HTTP method: {}", request.getMethod());
        }
        return null;
      }
      if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
        handleInvalidUpgradeHeader(request);
        return null;
      }
      List<String> connection = headers.getConnection();
      if (!connection.contains("Upgrade") && !connection.contains("upgrade")) {
        handleInvalidConnectHeader(request);
        return null;
      }
      if (!isWebSocketVersionSupported(headers)) {
        handleWebSocketVersionNotSupported(request);
        return null;
      }
      if (!isValidOrigin(request)) {
        request.setStatus(HttpStatus.FORBIDDEN);
        return null;
      }
      String wsKey = headers.getSecWebSocketKey();
      if (wsKey == null) {
        if (logger.isErrorEnabled()) {
          logger.error("Missing \"Sec-WebSocket-Key\" header");
        }
        request.setStatus(HttpStatus.BAD_REQUEST);
        return null;
      }
    }
    catch (IOException ex) {
      throw new HandshakeFailureException(
              "Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
    }

    String subProtocol = selectProtocol(headers.getSecWebSocketProtocol(), wsHandler);
    List<WebSocketExtension> requested = headers.getSecWebSocketExtensions();
    List<WebSocketExtension> supported = requestUpgradeStrategy.getSupportedExtensions(request);
    List<WebSocketExtension> extensions = filterRequestedExtensions(request, requested, supported);

    if (logger.isTraceEnabled()) {
      logger.trace("Upgrading to WebSocket, subProtocol={}, extensions={}", subProtocol, extensions);
    }

    return requestUpgradeStrategy.upgrade(request, subProtocol, extensions, wsHandler, attributes);
  }

  protected void handleInvalidUpgradeHeader(RequestContext request) throws IOException {
    if (logger.isErrorEnabled()) {
      logger.error(LogFormatUtils.formatValue(
              "Handshake failed due to invalid Upgrade header: " + request.getHeaders().getUpgrade(),
              -1, true));
    }
    request.setStatus(HttpStatus.BAD_REQUEST);
    request.getOutputStream()
            .write("Can \"Upgrade\" only to \"WebSocket\".".getBytes(StandardCharsets.UTF_8));
  }

  protected void handleInvalidConnectHeader(RequestContext request) throws IOException {
    if (logger.isErrorEnabled()) {
      logger.error(LogFormatUtils.formatValue(
              "Handshake failed due to invalid Connection header" + request.getHeaders().getConnection(), -1, true));
    }
    request.setStatus(HttpStatus.BAD_REQUEST);
    request.getOutputStream()
            .write("\"Connection\" must be \"upgrade\".".getBytes(StandardCharsets.UTF_8));
  }

  protected boolean isWebSocketVersionSupported(WebSocketHttpHeaders httpHeaders) {
    String version = httpHeaders.getSecWebSocketVersion();
    String[] supportedVersions = getSupportedVersions();
    for (String supportedVersion : supportedVersions) {
      if (supportedVersion.trim().equals(version)) {
        return true;
      }
    }
    return false;
  }

  protected String[] getSupportedVersions() {
    return this.requestUpgradeStrategy.getSupportedVersions();
  }

  protected void handleWebSocketVersionNotSupported(RequestContext request) {
    if (logger.isErrorEnabled()) {
      String version = request.getHeaders().getFirst("Sec-WebSocket-Version");
      logger.error(LogFormatUtils.formatValue(
              "Handshake failed due to unsupported WebSocket version: " + version +
                      ". Supported versions: " + Arrays.toString(getSupportedVersions()), -1, true));
    }
    request.setStatus(HttpStatus.UPGRADE_REQUIRED);
    request.setHeader(WebSocketHttpHeaders.SEC_WEBSOCKET_VERSION,
            StringUtils.arrayToCommaDelimitedString(getSupportedVersions()));
  }

  /**
   * Return whether the request {@code Origin} header value is valid or not.
   * By default, all origins as considered as valid. Consider using an
   * {@link OriginHandshakeInterceptor} for filtering origins if needed.
   */
  protected boolean isValidOrigin(RequestContext request) {
    return true;
  }

  /**
   * Perform the sub-protocol negotiation based on requested and supported sub-protocols.
   * For the list of supported sub-protocols, this method first checks if the target
   * WebSocketHandler is a {@link SubProtocolCapable} and then also checks if any
   * sub-protocols have been explicitly configured with
   * {@link #setSupportedProtocols(String...)}.
   *
   * @param requestedProtocols the requested sub-protocols
   * @param webSocketHandler the WebSocketHandler that will be used
   * @return the selected protocols or {@code null}
   * @see #determineHandlerSupportedProtocols(WebSocketHandler)
   */
  @Nullable
  protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
    List<String> handlerProtocols = determineHandlerSupportedProtocols(webSocketHandler);
    for (String protocol : requestedProtocols) {
      if (handlerProtocols.contains(protocol.toLowerCase())) {
        return protocol;
      }
      if (this.supportedProtocols.contains(protocol.toLowerCase())) {
        return protocol;
      }
    }
    return null;
  }

  /**
   * Determine the sub-protocols supported by the given WebSocketHandler by
   * checking whether it is an instance of {@link SubProtocolCapable}.
   *
   * @param handler the handler to check
   * @return a list of supported protocols, or an empty list if none available
   */
  protected final List<String> determineHandlerSupportedProtocols(WebSocketHandler handler) {
    WebSocketHandler handlerToCheck = handler.getRawHandler();
    List<String> subProtocols = null;
    if (handlerToCheck instanceof SubProtocolCapable) {
      subProtocols = ((SubProtocolCapable) handlerToCheck).getSubProtocols();
    }
    return (subProtocols != null ? subProtocols : Collections.emptyList());
  }

  /**
   * Filter the list of requested WebSocket extensions.
   * <p>the default implementation of this method filters the list to
   * leave only extensions that are both requested and supported.
   *
   * @param request the current request
   * @param requestedExtensions the list of extensions requested by the client
   * @param supportedExtensions the list of extensions supported by the server
   * @return the selected extensions or an empty list
   */
  protected List<WebSocketExtension> filterRequestedExtensions(RequestContext request,
          List<WebSocketExtension> requestedExtensions, List<WebSocketExtension> supportedExtensions) {

    ArrayList<WebSocketExtension> result = new ArrayList<>(requestedExtensions.size());
    for (WebSocketExtension extension : requestedExtensions) {
      if (supportedExtensions.contains(extension)) {
        result.add(extension);
      }
    }
    return result;
  }

  private static RequestUpgradeStrategy initRequestUpgradeStrategy() {
    var upgradeStrategy = TodayStrategies.findFirst(RequestUpgradeStrategy.class, null);
    if (upgradeStrategy != null) {
      return upgradeStrategy;
    }
    if (tomcatWsPresent) {
      return new TomcatRequestUpgradeStrategy();
    }
    else if (jettyWsPresent) {
      return new JettyRequestUpgradeStrategy();
    }
    else if (undertowWsPresent) {
      return new UndertowRequestUpgradeStrategy();
    }
    else {
      // Let's assume Jakarta WebSocket API 2.1+
      return new StandardWebSocketUpgradeStrategy();
    }
  }

}
