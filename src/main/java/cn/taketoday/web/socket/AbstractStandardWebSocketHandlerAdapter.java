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

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.HttpHeaders;

/**
 * javax.websocket
 *
 * @author TODAY 2021/5/3 23:17
 */
public abstract class AbstractStandardWebSocketHandlerAdapter extends AbstractWebSocketHandlerAdapter {

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    return new DefaultWebSocketSession(context, handler);
  }

  @Override
  protected void upgrade(RequestContext context, WebSocketSession ses, WebSocketHandler handler)
          throws HandshakeFailedException, IOException {
    super.upgrade(context, ses, handler);

    final HttpHeaders requestHeaders = context.requestHeaders();
    final HttpHeaders responseHeaders = context.responseHeaders();
    final ServerEndpointConfig endpointConfig = handler.getEndpointConfig();

    // Sub-protocols
    List<String> subProtocols = UpgradeUtils.getTokensFromHeader(requestHeaders, HttpHeaders.SEC_WEBSOCKET_PROTOCOL);
    String subProtocol = endpointConfig.getConfigurator().getNegotiatedSubprotocol(endpointConfig.getSubprotocols(), subProtocols);
    if (StringUtils.isNotEmpty(subProtocol)) {
      // RFC6455 4.2.2 explicitly states "" is not valid here
      responseHeaders.set(HttpHeaders.SEC_WEBSOCKET_PROTOCOL, subProtocol);
    }

    final DefaultWebSocketSession session = (DefaultWebSocketSession) ses;
    final ServerContainer webSocketContainer = getServerContainer();
//    ContainerProvider.getWebSocketContainer();
    doUpgrade(webSocketContainer, context, session, handler, subProtocol);

    handler.handshake(context);
  }

  protected abstract ServerContainer getServerContainer();

  protected abstract void doUpgrade(ServerContainer webSocketContainer, RequestContext context, DefaultWebSocketSession session,
                                    WebSocketHandler handler, String subProtocol);

}
