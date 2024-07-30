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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;

/**
 * Netty websocket session
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/5/24 21:03
 */
public class NettyWebSocketSession extends WebSocketSession {

  private final boolean secure;

  private final Channel channel;

  protected NettyWebSocketSession(boolean secure, Channel channel) {
    this.secure = secure;
    this.channel = channel;
  }

  @Override
  public void sendMessage(Message<?> message) throws IOException {
    if (message instanceof TextMessage) {
      sendText((String) message.getPayload());
    }
    else if (message instanceof BinaryMessage bm) {
      sendBinary(bm.getPayload());
    }
    else if (message instanceof PingMessage pm) {
      channel.writeAndFlush(new PingWebSocketFrame(Unpooled.wrappedBuffer(pm.getPayload())));
    }
    else if (message instanceof PongMessage pm) {
      channel.writeAndFlush(new PongWebSocketFrame(Unpooled.wrappedBuffer(pm.getPayload())));
    }
    else {
      throw new IllegalStateException("Unexpected WebSocket message type: " + message);
    }
  }

  @Override
  public void sendText(CharSequence text) {
    if (text.isEmpty()) {
      channel.writeAndFlush(new TextWebSocketFrame(Unpooled.EMPTY_BUFFER));
    }
    else {
      channel.writeAndFlush(new TextWebSocketFrame(Unpooled.copiedBuffer(text, CharsetUtil.UTF_8)));
    }
  }

  @Override
  public void sendBinary(ByteBuffer payload) {
    channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(payload)));
  }

  @Override
  public void sendPing() {
    channel.writeAndFlush(new PingWebSocketFrame());
  }

  @Override
  public void sendPong() {
    channel.writeAndFlush(new PongWebSocketFrame());
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
  public void close(CloseStatus status) throws IOException {
    channel.writeAndFlush(new CloseWebSocketFrame(status.getCode(), status.getReason()))
            .addListener(ChannelFutureListener.CLOSE);
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
