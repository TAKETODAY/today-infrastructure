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

import org.junit.jupiter.api.Test;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.logging.Logger;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NettyWebSocketSessionLifecycleTests {

  private NettyWebSocketSession session(Channel channel) {
    return new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);
  }

  @Test
  void closeIsIdempotentAndRejectsSends() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    try {
      NettyWebSocketSession session = session(channel);
      Future<Void> close = session.close();
      assertThat(session.close(CloseStatus.GOING_AWAY)).isSameAs(close);
      close.get(5, TimeUnit.SECONDS);
      assertThat(channel.isOpen()).isFalse();
      CloseWebSocketFrame frame = channel.readOutbound();
      assertThat(frame.statusCode()).isEqualTo(1000);
      frame.release();
      Object extraFrame = channel.readOutbound();
      assertThat(extraFrame).isNull();

      var payload = session.bufferFactory().copiedBuffer("late");
      Future<Void> send = session.sendBinary(payload);
      assertThat(send.getCause()).isInstanceOf(ClosedChannelException.class);
      assertThat(payload.isAllocated()).isFalse();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void abortInterruptsPendingClose() throws Exception {
    Channel channel = mock(Channel.class);
    given(channel.isOpen()).willReturn(true);
    DefaultChannelPromise write = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    DefaultChannelPromise disconnect = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willAnswer(invocation -> {
      ReferenceCountUtil.release(invocation.getArgument(0));
      return write;
    });
    given(channel.close()).willReturn(disconnect);
    NettyWebSocketSession session = session(channel);
    Future<Void> close = session.close();
    Future<Void> abort = session.abort();
    assertThat(close.isCancelled()).isTrue();
    assertThat(session.abort()).isSameAs(abort);
    disconnect.setSuccess();
    abort.get(5, TimeUnit.SECONDS);
    write.setFailure(new ClosedChannelException());
    verify(channel).close();
  }

  @Test
  void closingAlreadyClosedConnectionDoesNotWrite() {
    Channel channel = mock(Channel.class);
    assertThat(session(channel).close().isSuccess()).isTrue();
    verify(channel, never()).writeAndFlush(any());
  }

  @Test
  void closeTimeoutForcesDisconnect() throws Exception {
    Channel channel = mock(Channel.class);
    given(channel.isOpen()).willReturn(true);
    DefaultChannelPromise write = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    DefaultChannelPromise disconnect = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    disconnect.setSuccess();
    given(channel.writeAndFlush(any())).willAnswer(invocation -> {
      ReferenceCountUtil.release(invocation.getArgument(0));
      return write;
    });
    given(channel.close()).willReturn(disconnect);
    NettyWebSocketSession session = session(channel);
    session.setCloseTimeout(Duration.ofMillis(20));
    assertThatThrownBy(() -> session.close().get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(TimeoutException.class);
    session.abort().get(5, TimeUnit.SECONDS);
    verify(channel).close();
  }

  @Test
  void synchronousWriteAndCloseFailuresArePreserved() {
    Channel channel = mock(Channel.class);
    given(channel.isOpen()).willReturn(true);
    RuntimeException writeFailure = new IllegalStateException("write");
    RuntimeException closeFailure = new IllegalStateException("close");
    given(channel.writeAndFlush(any())).willThrow(writeFailure);
    given(channel.close()).willThrow(closeFailure);
    Future<Void> result = session(channel).close();
    assertThat(result.getCause()).isSameAs(writeFailure);
    assertThat(writeFailure.getSuppressed()).containsExactly(closeFailure);
  }

  @Test
  void concurrentCloseCallsSubmitOnlyOneCloseFrame() throws Exception {
    Channel channel = mock(Channel.class);
    given(channel.isOpen()).willReturn(true);
    DefaultChannelPromise write = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    DefaultChannelPromise disconnect = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willAnswer(invocation -> {
      ReferenceCountUtil.release(invocation.getArgument(0));
      return write;
    });
    given(channel.close()).willReturn(disconnect);
    NettyWebSocketSession session = session(channel);
    var executor = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      var first = executor.submit(() -> {
        start.await();
        return session.close();
      });
      var second = executor.submit(() -> {
        start.await();
        return session.close(CloseStatus.GOING_AWAY);
      });
      start.countDown();
      assertThat(first.get(5, TimeUnit.SECONDS)).isSameAs(second.get(5, TimeUnit.SECONDS));
      verify(channel).writeAndFlush(any());
      var payload = session.bufferFactory().copiedBuffer("late");
      assertThat(session.sendBinary(payload).getCause()).isInstanceOf(ClosedChannelException.class);
      assertThat(payload.isAllocated()).isFalse();
      write.setSuccess();
      disconnect.setSuccess();
    }
    finally {
      session.abort();
      executor.shutdownNow();
    }
  }

  @Test
  void cancellingCloseFutureDoesNotDisableForcedDisconnect() throws Exception {
    Channel channel = mock(Channel.class);
    given(channel.isOpen()).willReturn(true);
    DefaultChannelPromise write = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    DefaultChannelPromise disconnect = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
    CountDownLatch closed = new CountDownLatch(1);
    given(channel.writeAndFlush(any())).willAnswer(invocation -> {
      ReferenceCountUtil.release(invocation.getArgument(0));
      return write;
    });
    given(channel.close()).willAnswer(invocation -> {
      closed.countDown();
      disconnect.setSuccess();
      return disconnect;
    });
    NettyWebSocketSession session = session(channel);
    session.setCloseTimeout(Duration.ofMillis(20));
    Future<Void> result = session.close();
    result.cancel(false);
    assertThat(closed.await(5, TimeUnit.SECONDS)).isTrue();
    session.abort().get(5, TimeUnit.SECONDS);
    assertThat(result.isCancelled()).isTrue();
  }

  @Test
  void successfulSendReleasesPayloadThroughEncoder() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel(new WebSocket13FrameEncoder(false));
    try {
      NettyWebSocketSession session = session(channel);
      var payload = session.bufferFactory().copiedBuffer("hello");
      session.sendBinary(payload).get(5, TimeUnit.SECONDS);
      Object encoded;
      while ((encoded = channel.readOutbound()) != null) {
        ReferenceCountUtil.release(encoded);
      }
      assertThat(payload.isAllocated()).isFalse();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void failedSendOnClosedChannelReleasesPayload() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    try {
      channel.close().sync();
      NettyWebSocketSession session = session(channel);
      var payload = session.bufferFactory().copiedBuffer("hello");
      assertThatThrownBy(() -> session.sendBinary(payload).get(5, TimeUnit.SECONDS))
              .hasCauseInstanceOf(ClosedChannelException.class);
      assertThat(payload.isAllocated()).isFalse();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void asyncFailureClosesSessionAndReleasesInboundPayload() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    try {
      NettyWebSocketSession session = session(channel);
      Promise<Void> handling = Future.forPromise(Runnable::run);
      TextWebSocketFrame frame = new TextWebSocketFrame("hello");
      WebSocketHandler handler = mock(WebSocketHandler.class);
      given(handler.handleMessage(any(), any())).willReturn(handling);
      session.handleMessage(handler, frame, mock(Logger.class));
      assertThat(frame.refCnt()).isEqualTo(1);
      handling.setFailure(new IllegalStateException("handler"));
      session.close().get(5, TimeUnit.SECONDS);
      assertThat(frame.refCnt()).isZero();
      CloseWebSocketFrame close = channel.readOutbound();
      assertThat(close.statusCode()).isEqualTo(1011);
      close.release();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void retainedEchoSurvivesInboundCompletion() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    try {
      NettyWebSocketSession session = session(channel);
      BinaryWebSocketFrame frame = new BinaryWebSocketFrame(ByteBufAllocator.DEFAULT.buffer().writeByte(1));
      WebSocketHandler handler = mock(WebSocketHandler.class);
      given(handler.handleMessage(any(), any())).willAnswer(invocation -> {
        WebSocketMessage message = invocation.getArgument(1);
        session.send(message.retainedDuplicate());
        Promise<Void> completed = Future.forPromise(Runnable::run);
        completed.setSuccess(null);
        return completed;
      });
      session.handleMessage(handler, frame, mock(Logger.class));
      BinaryWebSocketFrame echo = channel.readOutbound();
      assertThat(echo.content().readByte()).isEqualTo((byte) 1);
      echo.release();
      assertThat(frame.refCnt()).isZero();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void asyncSuccessReleasesInboundPayloadOnlyAfterCompletion() {
    EmbeddedChannel channel = new EmbeddedChannel();
    try {
      NettyWebSocketSession session = session(channel);
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
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void synchronousHandlerFailureReleasesInboundPayload() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    try {
      NettyWebSocketSession session = session(channel);
      TextWebSocketFrame frame = new TextWebSocketFrame("hello");
      WebSocketHandler handler = mock(WebSocketHandler.class);
      given(handler.handleMessage(any(), any())).willThrow(new IllegalStateException("handler"));
      session.handleMessage(handler, frame, mock(Logger.class));
      session.close().get(5, TimeUnit.SECONDS);
      assertThat(frame.refCnt()).isZero();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }
}
