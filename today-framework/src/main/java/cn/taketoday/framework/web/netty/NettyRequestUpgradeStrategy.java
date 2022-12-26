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

package cn.taketoday.framework.web.netty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/22 21:43
 */
public class NettyRequestUpgradeStrategy implements RequestUpgradeStrategy {
  public static AttributeKey<WebSocketHandler> SOCKET_HANDLER_KEY = AttributeKey.valueOf("WebSocketHandler");
  public static AttributeKey<WebSocketSession> SOCKET_SESSION_KEY = AttributeKey.valueOf("WebSocketSession");

  private static final String[] SUPPORTED_VERSIONS = new String[] { "13" };

  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    if (!(context instanceof NettyRequestContext nettyContext)) {
      throw new IllegalStateException("not running in netty");
    }
    ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    String scheme = nettyContext.getScheme();
    return new NettyWebSocketSession(context.getHeaders(),
            Constant.HTTPS.equals(scheme) || "wss".equals(scheme), channelContext);
  }

  @Override
  public String[] getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }

  @Override
  public List<WebSocketExtension> getSupportedExtensions(RequestContext request) {
    return Collections.emptyList();
  }

  @Override
  public void upgrade(RequestContext context, @Nullable String selectedProtocol,
          List<WebSocketExtension> selectedExtensions, WebSocketHandler wsHandler,
          Map<String, Object> attributes) throws HandshakeFailureException {

    WebSocketSession session = createSession(context, wsHandler);

    NettyRequestContext nettyContext = (NettyRequestContext) context; // just cast
    FullHttpRequest request = nettyContext.nativeRequest();
    ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    var wsFactory = new WebSocketServerHandshakerFactory(request.uri(), null, true); // TODO subprotocols
    WebSocketServerHandshaker handShaker = wsFactory.newHandshaker(request);
    Channel channel = channelContext.channel();
    if (handShaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
    }
    else {
      channel.attr(SOCKET_HANDLER_KEY).set(wsHandler);
      channel.attr(SOCKET_SESSION_KEY).set(session);
      HttpHeaders responseHeaders = nettyContext.responseHeaders();
      handShaker.handshake(channel, request,
              ((NettyHttpHeaders) responseHeaders).headers, channel.newPromise());
    }
  }

}
