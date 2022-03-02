/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.web.BadRequestException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.AbstractHandlerAdapter;
import cn.taketoday.web.handler.HandlerAdapter;

/**
 * @author TODAY 2021/4/5 14:04
 * @since 3.0
 */
public abstract class AbstractWebSocketHandlerAdapter extends AbstractHandlerAdapter implements HandlerAdapter {
  protected static final String WS_VERSION_HEADER_VALUE = "13";

  @Override
  public boolean supports(Object handler) {
    return handler instanceof WebSocketHandler;
  }

  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    return handleInternal(context, (WebSocketHandler) handler);
  }

  protected Object handleInternal(RequestContext context, WebSocketHandler handler) throws Throwable {
    WebSocketSession session = createSession(context, handler);
    // do handshake
    doHandshake(context, session, handler);
    // call afterHandshake
    handler.afterHandshake(context, session);
    return NONE_RETURN_VALUE;
  }

  protected void doHandshake(
          RequestContext context, WebSocketSession session, WebSocketHandler handler) throws Throwable {

    // Validate the rest of the headers and reject the request if that validation fails
    List<String> connection = context.requestHeaders().getConnection();
    if (!UpgradeUtils.headerContainsToken(connection, HttpHeaders.UPGRADE)) {
      throw new BadRequestException("Not a WebSocket request");
    }

    HttpHeaders requestHeaders = context.requestHeaders();
    HttpHeaders responseHeaders = context.responseHeaders();
    if (!UpgradeUtils.headerContainsToken(requestHeaders, HttpHeaders.SEC_WEBSOCKET_VERSION, WS_VERSION_HEADER_VALUE)) {
      context.setStatus(HttpStatus.UPGRADE_REQUIRED);
      responseHeaders.set(HttpHeaders.SEC_WEBSOCKET_VERSION, WS_VERSION_HEADER_VALUE);
      return;
    }

    String key = requestHeaders.getFirst(HttpHeaders.SEC_WEBSOCKET_KEY);
    if (key == null) {
      throw new BadRequestException("WebSocket Key not found");
    }

    // If we got this far, all is good. Accept the connection.
    responseHeaders.setUpgrade(HttpHeaders.WEBSOCKET);
    responseHeaders.setConnection(HttpHeaders.UPGRADE);
    responseHeaders.set(HttpHeaders.SEC_WEBSOCKET_ACCEPT, UpgradeUtils.getWebSocketAccept(key));

    // Sub-protocols
    List<String> requested = UpgradeUtils.getTokensFromHeader(requestHeaders, HttpHeaders.SEC_WEBSOCKET_PROTOCOL);
    List<String> supported = getSupportedSubProtocols(context);
    String subProtocol = getNegotiatedSubProtocol(supported, requested);
    if (StringUtils.isNotEmpty(subProtocol)) {
      // RFC6455 4.2.2 explicitly states "" is not valid here
      responseHeaders.set(HttpHeaders.SEC_WEBSOCKET_PROTOCOL, subProtocol);
    }

    // Output required by RFC2616. Protocol specific headers should have
    // already been set.
    context.setStatus(HttpStatus.SWITCHING_PROTOCOLS);

    // Extensions
    List<WebSocketExtension> installedExtensions = getInstalledExtensions();
    // Should normally only be one header but handle the case of multiple headers
    List<WebSocketExtension> requestedExtensions = new ArrayList<>();
    List<String> extHeaders = requestHeaders.get(HttpHeaders.SEC_WEBSOCKET_EXTENSIONS);
    if (extHeaders != null) {
      for (String extHeader : extHeaders) {
        UpgradeUtils.parseExtensionHeader(requestedExtensions, extHeader);
      }
    }
    List<WebSocketExtension> supportedExtensions = getNegotiatedExtensions(installedExtensions, requestedExtensions);
    try {
      doUpgrade(context, session, handler, subProtocol, supportedExtensions);
    }
    catch (HandshakeFailedException e) {
      throw e;
    }
    catch (Exception e) {
      throw new HandshakeFailedException("Failed to upgrade", e);
    }
  }

  protected abstract WebSocketSession createSession(RequestContext context, WebSocketHandler handler);

  protected List<WebSocketExtension> getInstalledExtensions() {
    return Collections.emptyList();
  }

  protected void doUpgrade(
          RequestContext context, WebSocketSession session, WebSocketHandler handler, String subProtocol,
          List<WebSocketExtension> supportedExtensions) throws IOException {

  }

  protected List<String> getSupportedSubProtocols(RequestContext context) {
    return Collections.emptyList();
  }

  /**
   * Return the subprotocol the server endpoint has chosen from the requested
   * list supplied by a client who wishes to connect, or none if there wasn't one
   * this server endpoint liked. See
   * <a href="http://tools.ietf.org/html/rfc6455#section-4.2.2">Sending the
   * Server's Opening Handshake</a>. Subclasses may provide custom algorithms
   * based on other factors.
   *
   * <p>The default platform implementation of this method returns the first
   * subprotocol in the list sent by the client that the server supports,
   * or the empty string if there isn't one.
   *
   * @param requested the requested subprotocols from the client endpoint
   * @param supported the subprotocols supported by the server endpoint
   * @return the negotiated subprotocol or the empty string if there isn't one.
   */
  public String getNegotiatedSubProtocol(List<String> supported, List<String> requested) {
    if (CollectionUtils.isNotEmpty(supported)) {
      for (String request : requested) {
        if (supported.contains(request)) {
          return request;
        }
      }
    }
    return Constant.BLANK;
  }

  /**
   * Return the ordered list of extensions that t server endpoint will support
   * given the requested extension list passed in, the empty list if none. See
   * <a href="http://tools.ietf.org/html/rfc6455#section-9.1">Negotiating Extensions</a>
   *
   * <p>The default platform implementation of this method returns a list
   * containing all of the requested extensions passed to this method that
   * it supports, using the order in the requested extensions, the empty
   * list if none.
   *
   * @param installed the installed extensions on the implementation.
   * @param requested the requested extensions, in the order they were
   * requested by the client
   * @return the list of extensions negotiated, the empty list if none.
   */
  public List<WebSocketExtension> getNegotiatedExtensions(
          List<WebSocketExtension> installed, List<WebSocketExtension> requested) {
    LinkedHashSet<String> installedNames = new LinkedHashSet<>();
    for (WebSocketExtension e : installed) {
      installedNames.add(e.getName());
    }
    ArrayList<WebSocketExtension> result = new ArrayList<>();
    for (WebSocketExtension request : requested) {
      if (installedNames.contains(request.getName())) {
        result.add(request);
      }
    }
    return result;
  }

}
