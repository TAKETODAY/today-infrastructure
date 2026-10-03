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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.logging.Logger;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.util.ReferenceCountUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class NettyWebSocketSessionLifecycleTests {

  private final EmbeddedChannel channel = new EmbeddedChannel();

  private final NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
          new NettyDataBufferFactory(channel.alloc()), null);

  @AfterEach
  void releaseChannel() {
    channel.finishAndReleaseAll();
  }

  @Test
  void closeFutureObservesClosureWithoutInitiatingIt() throws Exception {
    Future<Void> result = session.closeFuture();
    assertThat(result.isDone()).isFalse();
    assertThat(channel.isOpen()).isTrue();
    assertThat(session.closeFuture()).isSameAs(result);
    session.close().get(5, TimeUnit.SECONDS);
    assertThat(result.get(5, TimeUnit.SECONDS)).isNull();
    assertThat(channel.isOpen()).isFalse();
    CloseWebSocketFrame frame = channel.readOutbound();
    assertThat(frame.statusCode()).isEqualTo(1000);
    frame.release();
  }

  @Test
  void closeFutureCanBeObtainedAfterRemoteDisconnect() throws Exception {
    channel.close().sync();
    assertThat(session.closeFuture().get(5, TimeUnit.SECONDS)).isNull();
  }

  @Test
  void abortCompletesClosureObservation() throws Exception {
    Future<Void> result = session.closeFuture();
    session.abort().get(5, TimeUnit.SECONDS);
    assertThat(result.get(5, TimeUnit.SECONDS)).isNull();
    assertThat(channel.outboundMessages()).isEmpty();
  }

  @Test
  void cancellingSharedObservationDoesNotCloseChannel() throws Exception {
    Future<Void> result = session.closeFuture();
    result.cancel(false);
    assertThat(channel.isOpen()).isTrue();
    assertThat(session.closeFuture()).isSameAs(result);
    session.abort().get(5, TimeUnit.SECONDS);
    assertThat(result.isCancelled()).isTrue();
  }

  @Test
  void failedCloseWriteLeavesConnectionOpen() throws Exception {
    IllegalStateException failure = new IllegalStateException("write");
    channel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
      @Override
      public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        ReferenceCountUtil.release(message);
        promise.setFailure(failure);
      }
    });
    Future<Void> observation = session.closeFuture();
    assertThatThrownBy(() -> session.close().get(5, TimeUnit.SECONDS)).hasCause(failure);
    assertThat(channel.isOpen()).isTrue();
    assertThat(observation.isDone()).isFalse();
    session.abort().get(5, TimeUnit.SECONDS);
    observation.get(5, TimeUnit.SECONDS);
  }

  @Test
  void successfulSendReleasesPayloadThroughEncoder() throws Exception {
    channel.pipeline().addLast(new WebSocket13FrameEncoder(false));
    var payload = session.bufferFactory().copiedBuffer("hello");
    session.sendBinary(payload).get(5, TimeUnit.SECONDS);
    Object encoded;
    while ((encoded = channel.readOutbound()) != null) {
      ReferenceCountUtil.release(encoded);
    }
    assertThat(payload.isAllocated()).isFalse();
  }

  @Test
  void failedSendOnClosedChannelReleasesPayload() throws Exception {
    channel.close().sync();
    var payload = session.bufferFactory().copiedBuffer("hello");
    assertThatThrownBy(() -> session.sendBinary(payload).get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(ClosedChannelException.class);
    assertThat(payload.isAllocated()).isFalse();
  }

  @Test
  void asyncFailureClosesSessionAndReleasesInboundPayload() throws Exception {
    Promise<Void> handling = Future.forPromise(Runnable::run);
    TextWebSocketFrame frame = new TextWebSocketFrame("hello");
    WebSocketHandler handler = mock(WebSocketHandler.class);
    given(handler.handleMessage(any(), any())).willReturn(handling);
    session.handleMessage(handler, frame, mock(Logger.class));
    assertThat(frame.refCnt()).isEqualTo(1);
    handling.setFailure(new IllegalStateException("handler"));
    session.closeFuture().get(5, TimeUnit.SECONDS);
    assertThat(frame.refCnt()).isZero();
    CloseWebSocketFrame close = channel.readOutbound();
    assertThat(close.statusCode()).isEqualTo(1011);
    close.release();
  }

  @Test
  void retainedEchoSurvivesInboundCompletion() {
    BinaryWebSocketFrame frame = new BinaryWebSocketFrame(channel.alloc().buffer().writeByte(1));
    WebSocketHandler handler = mock(WebSocketHandler.class);
    given(handler.handleMessage(any(), any())).willAnswer(invocation -> {
      WebSocketMessage message = invocation.getArgument(1);
      session.send(message.retainedDuplicate());
      return null;
    });
    session.handleMessage(handler, frame, mock(Logger.class));
    BinaryWebSocketFrame echo = channel.readOutbound();
    assertThat(echo.content().readByte()).isEqualTo((byte) 1);
    echo.release();
    assertThat(frame.refCnt()).isZero();
  }

  @Test
  void asyncSuccessReleasesInboundPayloadOnlyAfterCompletion() {
    Promise<Void> handling = Future.forPromise(Runnable::run);
    TextWebSocketFrame frame = new TextWebSocketFrame("hello");
    WebSocketHandler handler = mock(WebSocketHandler.class);
    given(handler.handleMessage(any(), any())).willReturn(handling);
    session.handleMessage(handler, frame, mock(Logger.class));
    assertThat(frame.refCnt()).isEqualTo(1);
    handling.setSuccess(null);
    assertThat(frame.refCnt()).isZero();
    assertThat(channel.isOpen()).isTrue();
  }

  @Test
  void synchronousHandlerFailureReleasesInboundPayload() throws Exception {
    TextWebSocketFrame frame = new TextWebSocketFrame("hello");
    WebSocketHandler handler = mock(WebSocketHandler.class);
    given(handler.handleMessage(any(), any())).willThrow(new IllegalStateException("handler"));
    session.handleMessage(handler, frame, mock(Logger.class));
    session.closeFuture().get(5, TimeUnit.SECONDS);
    assertThat(frame.refCnt()).isZero();
  }
}
