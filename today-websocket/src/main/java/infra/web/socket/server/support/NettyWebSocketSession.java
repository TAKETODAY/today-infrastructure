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

import java.net.InetSocketAddress;
import java.util.Objects;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Nullable;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

import static infra.web.socket.PromiseAdapter.adapt;

/**
 * Netty websocket session
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/5/24 21:03
 */
public class NettyWebSocketSession extends WebSocketSession {

  private final boolean secure;

  private final Channel channel;

  private final NettyDataBufferFactory allocator;

  protected NettyWebSocketSession(boolean secure, Channel channel, NettyDataBufferFactory allocator) {
    this.secure = secure;
    this.channel = channel;
    this.allocator = allocator;
  }

  @Override
  public NettyDataBufferFactory bufferFactory() {
    return allocator;
  }

  @Override
  public Future<Void> sendText(CharSequence text) {
    if (text.isEmpty()) {
      return send(new TextWebSocketFrame(Unpooled.EMPTY_BUFFER));
    }
    return send(new TextWebSocketFrame(Unpooled.copiedBuffer(text, CharsetUtil.UTF_8)));
  }

  @Override
  public Future<Void> sendBinary(DataBuffer payload) {
    return send(new BinaryWebSocketFrame(NettyDataBuffer.toByteBuf(payload)));
  }

  @Override
  public Future<Void> sendPing() {
    return send(new PingWebSocketFrame());
  }

  @Override
  public Future<Void> sendPong() {
    return send(new PongWebSocketFrame());
  }

  @Override
  public Future<Void> send(WebSocketMessage message) {
    return send(createFrame(message));
  }

  public Future<Void> send(WebSocketFrame message) {
    return adapt(channel.writeAndFlush(message));
  }

  protected WebSocketFrame createFrame(WebSocketMessage message) {
    if (message.getNativeMessage() != null) {
      return message.getNativeMessage();
    }

    ByteBuf byteBuf = NettyDataBuffer.toByteBuf(message.getPayload());
    return switch (message.getType()) {
      case PING -> new PingWebSocketFrame(byteBuf);
      case PONG -> new PongWebSocketFrame(byteBuf);
      case TEXT -> new TextWebSocketFrame(byteBuf);
      case BINARY -> new BinaryWebSocketFrame(byteBuf);
    };
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public boolean isActive() {
    return channel.isActive();
  }

  @Override
  public Future<Void> close(CloseStatus status) {
    return adapt(channel.writeAndFlush(new CloseWebSocketFrame(status.getCode(), status.getReason()))
            .addListener(ChannelFutureListener.CLOSE));
  }

  @Nullable
  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Nullable
  @Override
  public String getAcceptedProtocol() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final NettyWebSocketSession that))
      return false;
    return secure == that.secure
            && Objects.equals(channel, that.channel) && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel, secure);
  }

  @Override
  public String toString() {
    return "NettyWebSocketSession{channel=%s, secure=%s, attributes=%s}".formatted(channel, secure, attributes);
  }

}
