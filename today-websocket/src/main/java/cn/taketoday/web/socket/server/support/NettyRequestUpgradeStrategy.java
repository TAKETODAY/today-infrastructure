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

package cn.taketoday.web.socket.server.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.Decorator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.server.support.NettyRequestContext;
import cn.taketoday.web.socket.WebSocketExtension;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.server.HandshakeFailureException;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

/**
 * Netty RequestUpgradeStrategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/22 21:43
 */
public class NettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

  private static final String[] SUPPORTED_VERSIONS = new String[] { "13" };

  @Nullable
  private final Decorator<WebSocketSession> sessionDecorator;

  public NettyRequestUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    this.sessionDecorator = sessionDecorator;
  }

  protected WebSocketSession createSession(NettyRequestContext context, @Nullable Decorator<WebSocketSession> sessionDecorator) {
    WebSocketSession session = new NettyWebSocketSession(context.getHeaders(), context.config.secure, context.channelContext.channel());

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
          WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException //
  {
    if (!(context instanceof NettyRequestContext nettyContext)) {
      throw new IllegalStateException("not running in netty");
    }

    WebSocketSession session = createSession(nettyContext, sessionDecorator);

    if (!attributes.isEmpty()) {
      session.addAttributes(attributes);
    }

    FullHttpRequest request = nettyContext.nativeRequest();
    WebSocketServerHandshaker handShaker = createHandshakeFactory(request).newHandshaker(request);
    Channel channel = nettyContext.channelContext.channel();
    if (handShaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
      return null;
    }
    else {
      WebSocketHolder.bind(channel, wsHandler, session);
      handShaker.handshake(channel, request, nettyContext.nettyResponseHeaders, channel.newPromise())
              .addListener(future -> wsHandler.onOpen(session));
    }
    return session;
  }

  protected WebSocketServerHandshakerFactory createHandshakeFactory(FullHttpRequest request) {
    return new WebSocketServerHandshakerFactory(request.uri(), null, true);
  }

}
