/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.socket.server.support;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
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
import io.netty.util.ReferenceCountUtil;

import static infra.web.socket.CloseStatus.NO_CLOSE_FRAME;
import static infra.web.socket.CloseStatus.NO_STATUS_CODE;
import static infra.web.socket.PromiseAdapter.adapt;
import static infra.web.socket.handler.ExceptionWebSocketHandler.tryCloseWithError;

/**
 * Netty websocket session
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/5/24 21:03
 */
public class NettyWebSocketSession extends WebSocketSession {

  private static final Map<Class<?>, WebSocketMessage.Type> messageTypes = Map.of(
          TextWebSocketFrame.class, infra.web.socket.WebSocketMessage.Type.TEXT,
          PingWebSocketFrame.class, WebSocketMessage.Type.PING,
          PongWebSocketFrame.class, WebSocketMessage.Type.PONG,
          BinaryWebSocketFrame.class, WebSocketMessage.Type.BINARY);

  private final boolean secure;

  private final Channel channel;

  private final NettyDataBufferFactory allocator;

  @Nullable
  private final String acceptedProtocol;

  public NettyWebSocketSession(boolean secure, Channel channel,
          NettyDataBufferFactory allocator, @Nullable String acceptedProtocol) {
    this.secure = secure;
    this.channel = channel;
    this.allocator = allocator;
    this.acceptedProtocol = acceptedProtocol;
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
  public void abort() {
    channel.close();
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
  public InetSocketAddress getLocalAddress() {
    return (InetSocketAddress) channel.localAddress();
  }

  @Nullable
  @Override
  public String getAcceptedProtocol() {
    return acceptedProtocol;
  }

  /**
   * internal use only
   *
   * @see WebSocketHandler#onClose
   */
  public void onClose(WebSocketHandler handler, CloseStatus closeStatus, Logger logger) {
    try {
      Future<Void> future = handler.onClose(this, closeStatus);
      if (logger.isDebugEnabled()) {
        logger.debug("Exiting onClose {} returned {}", this, future);
      }
      if (channel.isActive()) {
        CloseStatus status;
        if (closeStatus.equalsCode(NO_STATUS_CODE) || closeStatus.equalsCode(NO_CLOSE_FRAME)) {
          status = CloseStatus.NORMAL;
          logger.debug("Using statusCode {} instead of {}", status, closeStatus);
        }
        else {
          status = closeStatus;
        }
        if (future != null) {
          future.onCompleted(f -> {
            logger.debug("Future returned by onClose completed result={} error={}", f.getNow(), f.getCause());
            close(status);
          });
        }
        else {
          close(status);
        }
      }
    }
    catch (Throwable ex) {
      logger.warn("Unhandled on-close exception for {}", this, ex);
    }
  }

  /**
   * internal use only
   *
   * @see WebSocketHandler#handleMessage(WebSocketSession, WebSocketMessage)
   */
  public void handleMessage(WebSocketHandler handler, WebSocketFrame frame, Logger logger) {
    try {
      WebSocketMessage.Type messageType = messageTypes.get(frame.getClass());
      Assert.state(messageType != null, "Unexpected message type");
      DataBuffer payload = allocator.wrap(frame.content());
      var message = new WebSocketMessage(messageType, payload, frame, frame.isFinalFragment());
      Future<Void> future = handler.handleMessage(this, message);
      if (future != null) {
        future.onFinally(message::release);
      }
      else {
        message.release();
      }
    }
    catch (Throwable e) {
      ReferenceCountUtil.safeRelease(frame);
      tryCloseWithError(this, e, logger);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final NettyWebSocketSession that))
      return false;
    return secure == that.secure
            && Objects.equals(channel, that.channel);
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
