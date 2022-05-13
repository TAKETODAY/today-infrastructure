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

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.AbstractWebSocketHandlerAdapter;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;

/**
 * The Netty websocket handler adapter
 *
 * @author TODAY 2021/5/24 21:02
 * @since 1.0.1
 */
public class NettyWebSocketHandlerAdapter extends AbstractWebSocketHandlerAdapter {
  public static final AttributeKey<WebSocketHandler> SOCKET_HANDLER_KEY = AttributeKey.valueOf("WebSocketHandler");
  public static final AttributeKey<WebSocketSession> SOCKET_SESSION_KEY = AttributeKey.valueOf("WebSocketSession");

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    if (!(context instanceof final NettyRequestContext nettyContext)) {
      throw new IllegalStateException("not running in netty");
    }
    final ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    final String scheme = nettyContext.getScheme();
    return new NettyWebSocketSession(
            Constant.HTTPS.equals(scheme) || "wss".equals(scheme), channelContext);
  }

  @Override
  protected void doHandshake(RequestContext context, WebSocketSession session, WebSocketHandler handler) {
    final NettyRequestContext nettyContext = (NettyRequestContext) context; // just cast
    final FullHttpRequest request = nettyContext.nativeRequest();
    final ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    final WebSocketServerHandshakerFactory wsFactory
            = new WebSocketServerHandshakerFactory(request.uri(), null, true); // TODO subprotocols
    final WebSocketServerHandshaker handShaker = wsFactory.newHandshaker(request);
    final Channel channel = channelContext.channel();
    if (handShaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
    }
    else {
      channel.attr(SOCKET_HANDLER_KEY).set(handler);
      channel.attr(SOCKET_SESSION_KEY).set(session);
      final HttpHeaders responseHeaders = nettyContext.responseHeaders();
      handShaker.handshake(channel, request, ((NettyHttpHeaders) responseHeaders).headers, channel.newPromise());
    }
  }
}
