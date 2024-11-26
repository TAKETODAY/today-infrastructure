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

package infra.web.socket.server.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.Decorator;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.server.support.NettyRequestContext;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.HandshakeFailureException;
import infra.web.socket.server.RequestUpgradeStrategy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
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

  private WebSocketDecoderConfig decoderConfig = WebSocketDecoderConfig.newBuilder()
          .maxFramePayloadLength(65536)
          .expectMaskedFrames(true)
          .allowMaskMismatch(false)
          .allowExtensions(false)
          .closeOnProtocolViolation(true)
          .withUTF8Validator(true)
          .build();

  public NettyRequestUpgradeStrategy(@Nullable Decorator<WebSocketSession> sessionDecorator) {
    this.sessionDecorator = sessionDecorator;
  }

  /**
   * set Frames decoder configuration.
   *
   * @param decoderConfig Frames decoder configuration.
   */
  public void setDecoderConfig(WebSocketDecoderConfig decoderConfig) {
    this.decoderConfig = decoderConfig;
  }

  protected WebSocketSession createSession(NettyRequestContext context, @Nullable Decorator<WebSocketSession> sessionDecorator) {
    WebSocketSession session = new NettyWebSocketSession(context.config.secure, context.channelContext.channel());

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
    session.setAttributes(attributes);

    FullHttpRequest request = nettyContext.nativeRequest();
    var handshaker = createHandshakeFactory(request, selectedProtocol, selectedExtensions).newHandshaker(request);
    Channel channel = nettyContext.channelContext.channel();
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
      return null;
    }

    WebSocketHolder.bind(channel, wsHandler, session);
    ChannelPromise writePromise = channel.newPromise();

    var handshakeChannel = new HandshakeChannel(channel, writePromise);
    ChannelFuture handshakeF = handshaker.handshake(handshakeChannel, request);
    if (handshakeF.isDone() && !handshakeF.isSuccess()) {
      throw new HandshakeFailureException("Handshake failed", handshakeF.cause());
    }

    FullHttpResponse response = handshakeChannel.response;
    Assert.state(response != null, "Handshake failed");

    nettyContext.setStatus(response.status());
    nettyContext.nettyResponseHeaders.add(response.headers());
    nettyContext.registerDestructionCallback(handshakeChannel);

    handshakeChannel.release();

    writePromise.addListener(future -> {
      if (future.isSuccess()) {
        wsHandler.onOpen(session);
      }
      else {
        wsHandler.onError(session, future.cause());
      }
    });
    return session;
  }

  protected WebSocketServerHandshakerFactory createHandshakeFactory(FullHttpRequest request,
          @Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions) {
    return new WebSocketServerHandshakerFactory(request.uri(), selectedProtocol, decoderConfig);
  }

}
