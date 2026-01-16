/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket.server.support;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import infra.beans.BeanUtils;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.LogFormatUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.socket.SubProtocolCapable;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketHttpHeaders;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.HandshakeFailureException;
import infra.web.socket.server.HandshakeHandler;
import infra.web.socket.server.RequestUpgradeStrategy;

/**
 * A base class for {@link HandshakeHandler} implementations.
 *
 * <p>Performs initial validation of the WebSocket handshake request - possibly rejecting it
 * through the appropriate HTTP status code - while also allowing its subclasses to override
 * various parts of the negotiation process (e.g. origin validation, sub-protocol negotiation,
 * extensions negotiation, etc).
 *
 * <p>If the negotiation succeeds, the actual upgrade is delegated to a server-specific
 * {@link infra.web.socket.server.RequestUpgradeStrategy}, which will update
 * the response as necessary and initialize the WebSocket.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final RequestUpgradeStrategy requestUpgradeStrategy;

  private final ArrayList<String> supportedProtocols = new ArrayList<>();

  /**
   * Default constructor that auto-detects and instantiates a
   * {@link RequestUpgradeStrategy} suitable for the runtime container.
   *
   * @throws IllegalStateException if no {@link RequestUpgradeStrategy} can be found.
   * @see #initRequestUpgradeStrategy()
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
      this.supportedProtocols.add(protocol.toLowerCase(Locale.ROOT));
    }
  }

  /**
   * Return the list of supported sub-protocols.
   */
  public String[] getSupportedProtocols() {
    return StringUtils.toStringArray(this.supportedProtocols);
  }

  @Nullable
  @Override
  public final WebSocketSession doHandshake(RequestContext request, WebSocketHandler wsHandler, Map<String, Object> attributes)
          throws HandshakeFailureException {

    WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
    if (logger.isTraceEnabled()) {
      logger.trace("Processing request {} with headers={}", request.getURI(), headers);
    }
    try {
      HttpMethod method = request.getMethod();
      if (HttpMethod.GET != method && method != HttpMethod.CONNECT) {
        request.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        request.responseHeaders().setAllow(Set.of(HttpMethod.GET, HttpMethod.CONNECT));
        if (logger.isDebugEnabled()) {
          logger.debug("Handshake failed due to unexpected HTTP method: {}", method);
        }
        return null;
      }

      if (HttpMethod.GET == method) {
        if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
          handleInvalidUpgradeHeader(request);
          return null;
        }
        List<String> connection = headers.getConnection();
        if (!connection.contains("Upgrade") && !connection.contains("upgrade")) {
          handleInvalidConnectHeader(request);
          return null;
        }
        String key = headers.getSecWebSocketKey();
        if (key == null) {
          if (logger.isErrorEnabled()) {
            logger.error("Missing \"Sec-WebSocket-Key\" header");
          }
          request.setStatus(HttpStatus.BAD_REQUEST);
          return null;
        }
      }

      if (!isWebSocketVersionSupported(headers)) {
        handleWebSocketVersionNotSupported(request);
        return null;
      }
      if (!isValidOrigin(request)) {
        request.setStatus(HttpStatus.FORBIDDEN);
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
      logger.error(LogFormatUtils.formatValue("Handshake failed due to invalid Connection header" +
              request.getHeaders().getConnection(), -1, true));
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
    return requestUpgradeStrategy.getSupportedVersions();
  }

  protected void handleWebSocketVersionNotSupported(RequestContext request) {
    if (logger.isErrorEnabled()) {
      String version = request.getHeaders().getFirst(HttpHeaders.SEC_WEBSOCKET_VERSION);
      logger.error(LogFormatUtils.formatValue("Handshake failed due to unsupported WebSocket version: %s. Supported versions: %s"
              .formatted(version, Arrays.toString(getSupportedVersions())), -1, true));
    }
    request.setStatus(HttpStatus.UPGRADE_REQUIRED);
    request.setHeader(HttpHeaders.SEC_WEBSOCKET_VERSION,
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
      if (handlerProtocols.contains(protocol.toLowerCase(Locale.ROOT))) {
        return protocol;
      }
      if (this.supportedProtocols.contains(protocol.toLowerCase(Locale.ROOT))) {
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
    List<String> subProtocols = null;
    if (handler instanceof SubProtocolCapable spc) {
      subProtocols = spc.getSubProtocols();
    }
    else {
      if (handler.getRawHandler() instanceof SubProtocolCapable spc) {
        subProtocols = spc.getSubProtocols();
      }
    }
    return subProtocols != null ? subProtocols : Collections.emptyList();
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
    if (requestedExtensions.isEmpty()) {
      return Collections.emptyList();
    }
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
    return BeanUtils.newInstance("infra.web.server.netty.NettyRequestUpgradeStrategy", ClassUtils.getDefaultClassLoader());
  }

}
