/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.DataSize;
import infra.util.ExceptionUtils;
import infra.web.RequestContext;
import infra.web.server.support.NettyRequestContext;
import infra.web.socket.WebSocketExtension;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.HandshakeFailureException;
import infra.web.socket.server.RequestUpgradeStrategy;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
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

  private final WebSocketDecoderConfig decoderConfig;

  public NettyRequestUpgradeStrategy() {
    this(WebSocketDecoderConfig.newBuilder()
            .maxFramePayloadLength(DataSize.ofKilobytes(512).bytes().intValue())
            .expectMaskedFrames(true)
            .allowMaskMismatch(false)
            .allowExtensions(false)
            .closeOnProtocolViolation(true)
            .withUTF8Validator(true)
            .build());
  }

  /**
   * Constructor specifying the websocket frames decoder options.
   *
   * @param decoderConfig Frames decoder options.
   * @since 5.0
   */
  public NettyRequestUpgradeStrategy(WebSocketDecoderConfig decoderConfig) {
    Assert.notNull(decoderConfig, "WebSocketDecoderConfig is required");
    this.decoderConfig = decoderConfig;
  }

  @Override
  public String[] getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }

  @Override
  public List<WebSocketExtension> getSupportedExtensions(RequestContext request) {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public WebSocketSession upgrade(RequestContext context, @Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
          WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException //
  {
    if (!(context instanceof NettyRequestContext nettyContext)) {
      throw new IllegalStateException("not running in netty");
    }

    HttpRequest request = nettyContext.nativeRequest();
    var handshaker = createHandshakeFactory(request, selectedExtensions).newHandshaker(request);
    Channel channel = nettyContext.channel;
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
      return null;
    }

    NettyDataBufferFactory allocator = new NettyDataBufferFactory(channel.alloc());
    NettyWebSocketSession session = createSession(selectedProtocol, nettyContext, allocator);
    session.setAttributes(attributes);

    WebSocketAttribute.bind(channel, wsHandler, session);
    ChannelPromise writePromise = channel.newPromise();

    var handshakeChannel = new HandshakeChannel(channel, writePromise);
    ChannelFuture handshakeF = handshaker.handshake(handshakeChannel,
            new DefaultFullHttpRequest(request.protocolVersion(), request.method(), request.uri(),
                    Unpooled.EMPTY_BUFFER, request.headers(), EmptyHttpHeaders.INSTANCE));
    if (handshakeF.isDone() && !handshakeF.isSuccess()) {
      throw new HandshakeFailureException("Handshake failed", handshakeF.cause());
    }

    FullHttpResponse response = handshakeChannel.response;
    Assert.state(response != null, "Handshake failed");

    nettyContext.setStatus(response.status());
    nettyContext.nettyResponseHeaders.add(response.headers());
    nettyContext.registerDestructionCallback(handshakeChannel);

    if (selectedProtocol != null) {
      nettyContext.nettyResponseHeaders.set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, selectedProtocol);
    }

    handshakeChannel.release();

    writePromise.addListener(future -> {
      try {
        if (future.isSuccess()) {
          wsHandler.onOpen(session);
        }
        else {
          wsHandler.onError(session, future.cause());
        }
      }
      catch (Throwable e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    });

    return session;
  }

  protected NettyWebSocketSession createSession(@Nullable String selectedProtocol,
          NettyRequestContext context, NettyDataBufferFactory allocator) {
    return new NettyWebSocketSession(context.config.secure, context.channel, allocator, selectedProtocol);
  }

  protected WebSocketServerHandshakerFactory createHandshakeFactory(HttpRequest request, List<WebSocketExtension> selectedExtensions) {
    return new WebSocketServerHandshakerFactory(request.uri(), null, decoderConfig);
  }

}
