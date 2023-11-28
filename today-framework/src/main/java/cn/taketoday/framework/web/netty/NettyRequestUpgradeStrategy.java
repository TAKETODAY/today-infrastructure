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

package cn.taketoday.framework.web.netty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.Decorator;
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
 * Netty RequestUpgradeStrategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/22 21:43
 */
public class NettyRequestUpgradeStrategy implements RequestUpgradeStrategy {
  static final AttributeKey<WebSocketHolder> WebSocketHolder = AttributeKey.valueOf("WebSocketHolder");

  private static final String[] SUPPORTED_VERSIONS = new String[] { "13" };

  @Nullable
  private final Decorator<WebSocketSession> sessionDecorator;

  public NettyRequestUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    this.sessionDecorator = sessionDecorator;
  }

  protected WebSocketSession createSession(RequestContext context, @Nullable Decorator<WebSocketSession> sessionDecorator) {
    if (!(context instanceof NettyRequestContext nettyContext)) {
      throw new IllegalStateException("not running in netty");
    }

    ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    String scheme = nettyContext.getScheme();

    WebSocketSession session = new NettyWebSocketSession(context.getHeaders(),
            Constant.HTTPS.equals(scheme) || "wss".equals(scheme), channelContext);

    if (sessionDecorator != null) {
      session = sessionDecorator.decorate(session);
    }
    return session;
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
  public WebSocketSession upgrade(RequestContext context, @Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
          WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

    WebSocketSession session = createSession(context, sessionDecorator);

    NettyRequestContext nettyContext = (NettyRequestContext) context; // just cast
    FullHttpRequest request = nettyContext.nativeRequest();
    ChannelHandlerContext channelContext = nettyContext.getChannelContext();
    var wsFactory = new WebSocketServerHandshakerFactory(request.uri(), null, true);
    WebSocketServerHandshaker handShaker = wsFactory.newHandshaker(request);
    Channel channel = channelContext.channel();
    if (handShaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
      return null;
    }
    else {
      channel.attr(WebSocketHolder).set(new WebSocketHolder(wsHandler, session));
      handShaker.handshake(channel, request, nettyContext.nettyResponseHeaders, channel.newPromise())
              .addListener(future -> wsHandler.onOpen(session));
    }
    return session;
  }

}
