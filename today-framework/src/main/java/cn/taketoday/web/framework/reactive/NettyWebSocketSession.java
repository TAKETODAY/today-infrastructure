package cn.taketoday.web.framework.reactive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.NativeWebSocketSession;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author TODAY 2021/5/24 21:03
 * @since 1.0.1
 */
public class NettyWebSocketSession extends NativeWebSocketSession<ChannelHandlerContext> {
  private final Channel channel;
  private final boolean secure;

  public NettyWebSocketSession(boolean secure, ChannelHandlerContext ctx) {
    this.secure = secure;
    this.channel = ctx.channel();
  }

  @Override
  public void sendText(String text) {
    channel.write(new TextWebSocketFrame(text));
  }

  @Override
  public void sendPartialText(String partialMessage, boolean isLast) {
    channel.write(new TextWebSocketFrame(isLast, 0, partialMessage));
  }

  @Override
  public void sendBinary(BinaryMessage data) {
    final ByteBuffer payload = data.getPayload();
    channel.write(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(payload)));
  }

  @Override
  public void sendPartialBinary(ByteBuffer partialByte, boolean isLast) {
    channel.write(new BinaryWebSocketFrame(isLast, 0, Unpooled.wrappedBuffer(partialByte)));
  }

  @Override
  public void sendPing(PingMessage message) {
    channel.write(new PingWebSocketFrame(Unpooled.wrappedBuffer(message.getPayload())));
  }

  @Override
  public void sendPong(PongMessage message) {
    channel.write(new PongWebSocketFrame(Unpooled.wrappedBuffer(message.getPayload())));
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
  public long getMaxIdleTimeout() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    channel.writeAndFlush(new CloseWebSocketFrame(status.getCode(), status.getReason()))
            .addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final NettyWebSocketSession that))
      return false;
    if (!super.equals(o))
      return false;
    return secure == that.secure && Objects.equals(channel, that.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel, secure);
  }

  @Override
  public String toString() {
    return "NettyWebSocketSession{" +
            "channel=" + channel +
            ", secure=" + secure +
            ", nativeSession=" + nativeSession +
            ", attributes=" + attributes +
            '}';
  }
}
