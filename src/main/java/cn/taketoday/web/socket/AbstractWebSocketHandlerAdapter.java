/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import java.util.List;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.handler.AbstractHandlerAdapter;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY 2021/4/5 14:04
 * @since 3.0
 */
public abstract class AbstractWebSocketHandlerAdapter extends AbstractHandlerAdapter implements HandlerAdapter {
  protected static final String WS_VERSION_HEADER_VALUE = "13";

  @Override
  public boolean supports(final Object handler) {
    return handler instanceof WebSocketHandler;
  }

  @Override
  public Object handle(final RequestContext context, final Object handler) throws Throwable {
    return handleInternal(context, (WebSocketHandler) handler);
  }

  protected Object handleInternal(RequestContext context, WebSocketHandler handler) throws Throwable {
    WebSocketSession session = createSession(context, handler);
    doHandshake(context, session, handler);
    return NONE_RETURN_VALUE;
  }

  protected abstract WebSocketSession createSession(RequestContext context, WebSocketHandler handler);

  protected void doHandshake(
          RequestContext context, WebSocketSession session, WebSocketHandler handler) throws HandshakeFailedException, IOException {

    // Validate the rest of the headers and reject the request if that validation fails
    final List<String> connection = context.requestHeaders().getConnection();
    if (!UpgradeUtils.headerContainsToken(connection, HttpHeaders.UPGRADE)) {
      throw new BadRequestException("Not a WebSocket request");
    }

    final HttpHeaders requestHeaders = context.requestHeaders();
    final HttpHeaders responseHeaders = context.responseHeaders();
    if (!UpgradeUtils.headerContainsToken(requestHeaders, HttpHeaders.SEC_WEBSOCKET_VERSION, WS_VERSION_HEADER_VALUE)) {
      context.setStatus(HttpStatus.UPGRADE_REQUIRED);
      responseHeaders.set(HttpHeaders.SEC_WEBSOCKET_VERSION, WS_VERSION_HEADER_VALUE);
      return;
    }

    final String key = requestHeaders.getFirst(HttpHeaders.SEC_WEBSOCKET_KEY);
    if (key == null) {
      throw new BadRequestException("WebSocket Key not found");
    }

    // If we got this far, all is good. Accept the connection.
    responseHeaders.setUpgrade(HttpHeaders.WEBSOCKET);
    responseHeaders.setConnection(HttpHeaders.UPGRADE);
    responseHeaders.set(HttpHeaders.SEC_WEBSOCKET_ACCEPT, UpgradeUtils.getWebSocketAccept(key));

  }

}
