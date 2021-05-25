package cn.taketoday.framework.reactive;

import java.io.IOException;
import java.nio.ByteBuffer;

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
  private final ChannelHandlerContext ctx;
  private final Channel channel;

  public NettyWebSocketSession(ChannelHandlerContext ctx) {
    this.ctx = ctx;
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
    Unpooled.wrappedBuffer(payload);
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
    return false;
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public long getMaxIdleTimeout() {
    return 0;
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {

  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {

  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return 0;
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {

  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return 0;
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    channel.writeAndFlush(new CloseWebSocketFrame(status.getCode(), status.getReason()))
            .addListener(ChannelFutureListener.CLOSE);
  }
}
