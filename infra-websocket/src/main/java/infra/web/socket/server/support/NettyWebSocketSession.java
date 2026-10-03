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
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.logging.Logger;
import infra.util.Assert;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import static infra.web.socket.CloseStatus.NO_CLOSE_FRAME;
import static infra.web.socket.CloseStatus.NO_STATUS_CODE;
import static infra.web.socket.PromiseAdapter.adapt;
import static infra.web.socket.handler.ExceptionWebSocketHandler.tryCloseWithError;

/**
 * Netty websocket session
 *
 * <p>Writes are submitted through the channel pipeline. Netty-backed outbound
 * payloads are passed to the pipeline without retaining them; ownership of that
 * reference transfers to Netty, which releases it after processing the write.
 * Retain a separate reference before sending if the payload is needed afterwards.
 * Other buffer implementations are copied when converted to Netty buffers.
 *
 * <p>Returned futures use the default infrastructure notification executor.
 * Cancelling them does not cancel the underlying channel operation.
 *
 * <p>Send submission and initiation of closure are serialized. Once closure
 * begins, new sends are rejected and their frames released. Repeated close calls
 * reuse the first close result and status. An abort during closure cancels the
 * graceful-close result and immediately initiates transport closure. Repeated
 * aborts reuse the transport-close result. Closing an already closed connection
 * succeeds without writing another close frame.
 *
 * <p>Inbound payloads remain valid until the handler's returned future completes,
 * or until the handler returns if it provides no future. Retain an independent
 * reference (for example with {@link WebSocketMessage#retainedDuplicate()}) before
 * forwarding an inbound message to an outbound write or keeping it afterwards.
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

  private final @Nullable String acceptedProtocol;

  private Duration closeTimeout = Duration.ofSeconds(10);

  private @Nullable Promise<Void> closeResult;

  private @Nullable Promise<Void> abortResult;

  private @Nullable ScheduledFuture<?> closeTimeoutTask;

  /**
   * Set the maximum duration of an initiated close operation. The default is
   * ten seconds. On timeout the close future fails and the connection is aborted.
   * Cancellation of the returned close future does not disable this timeout.
   *
   * @param timeout a positive close timeout, configured before closing
   */
  public synchronized void setCloseTimeout(Duration timeout) {
    Assert.isTrue(!timeout.isNegative() && !timeout.isZero(), "Close timeout must be positive");
    Assert.state(closeResult == null && abortResult == null, "Session is already closing");
    timeout.toNanos();
    closeTimeout = timeout;
  }

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

  public synchronized Future<Void> send(WebSocketFrame message) {
    if (closeResult != null || abortResult != null) {
      ReferenceCountUtil.safeRelease(message);
      return Future.failed(new ClosedChannelException());
    }
    try {
      return adapt(channel.writeAndFlush(message));
    }
    catch (Throwable ex) {
      ReferenceCountUtil.safeRelease(message);
      return Future.failed(ex);
    }
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
  public synchronized Future<Void> abort() {
    if (closeResult != null && !closeResult.isDone()) {
      closeResult.cancel(false);
    }
    cancelCloseTimeout();
    return closeConnection(null);
  }

  @Override
  public synchronized Future<Void> close(CloseStatus status) {
    if (closeResult != null) {
      return closeResult;
    }
    if (abortResult != null) {
      return abortResult;
    }
    CloseWebSocketFrame frame = new CloseWebSocketFrame(status.getCode(), status.getReason());
    Promise<Void> result = Future.forPromise();
    closeResult = result;
    if (!channel.isOpen()) {
      frame.release();
      result.trySuccess(null);
      return result;
    }
    try {
      closeTimeoutTask = GlobalEventExecutor.INSTANCE.schedule(() -> {
        synchronized (this) {
          if (abortResult == null || !abortResult.isDone()) {
            result.tryFailure(new TimeoutException("WebSocket close timed out"));
            closeConnection(null);
          }
        }
      }, closeTimeout.toNanos(), TimeUnit.NANOSECONDS);
      channel.writeAndFlush(frame).addListener(writeFuture -> closeConnection(writeFuture.cause()));
    }
    catch (Throwable ex) {
      ReferenceCountUtil.safeRelease(frame);
      closeConnection(ex);
    }
    return result;
  }

  private synchronized Future<Void> closeConnection(@Nullable Throwable writeFailure) {
    if (abortResult != null) {
      return abortResult;
    }
    Promise<Void> result = Future.forPromise();
    abortResult = result;
    try {
      channel.close().addListener(future -> finishClose(result, writeFailure, future.cause()));
    }
    catch (Throwable ex) {
      finishClose(result, writeFailure, ex);
    }
    return result;
  }

  private synchronized void finishClose(Promise<Void> result,
          @Nullable Throwable writeFailure, @Nullable Throwable closeFailure) {
    cancelCloseTimeout();
    if (closeFailure instanceof CancellationException) {
      result.cancel(closeFailure, false);
    }
    else if (closeFailure != null) {
      result.tryFailure(closeFailure);
    }
    else {
      result.trySuccess(null);
    }
    if (closeResult != null) {
      if (writeFailure != null) {
        if (closeFailure != null && closeFailure != writeFailure) {
          writeFailure.addSuppressed(closeFailure);
        }
        if (writeFailure instanceof CancellationException) {
          closeResult.cancel(writeFailure, false);
        }
        else {
          closeResult.tryFailure(writeFailure);
        }
      }
      else if (closeFailure instanceof CancellationException) {
        closeResult.cancel(closeFailure, false);
      }
      else if (closeFailure != null) {
        closeResult.tryFailure(closeFailure);
      }
      else {
        closeResult.trySuccess(null);
      }
    }
  }

  private void cancelCloseTimeout() {
    if (closeTimeoutTask != null) {
      closeTimeoutTask.cancel(false);
      closeTimeoutTask = null;
    }
  }

  @Override
  public @Nullable InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Override
  public @Nullable InetSocketAddress getLocalAddress() {
    return (InetSocketAddress) channel.localAddress();
  }

  @Override
  public @Nullable String getAcceptedProtocol() {
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
        future.onCompleted(completed -> {
          try {
            if (completed.getCause() != null) {
              tryCloseWithError(this, completed.getCause(), logger);
            }
          }
          finally {
            message.release();
          }
        });
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
  public boolean equals(@Nullable Object o) {
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
