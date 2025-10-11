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

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.logging.Logger;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/14 21:58
 */
class NettyWebSocketSessionTests {

  @Test
  void close() throws InterruptedException {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    promise.setSuccess();
    given(channel.writeAndFlush(any())).willReturn(promise);
    given(channel.isActive()).willReturn(true);
    given(channel.isOpen()).willReturn(true);
    given(channel.localAddress()).willReturn(InetSocketAddress.createUnresolved("localhost", 1234));
    given(channel.remoteAddress()).willReturn(InetSocketAddress.createUnresolved("localhost", 1234));

    assertThat(session.isSecure()).isFalse();
    assertThat(session.isOpen()).isTrue();
    assertThat(session.isActive()).isTrue();
    assertThat(session.bufferFactory()).isNotNull();
    assertThat(session.getAcceptedProtocol()).isNull();

    assertThat(session.getLocalAddress()).isEqualTo(channel.localAddress()).isEqualTo(InetSocketAddress.createUnresolved("localhost", 1234));
    assertThat(session.getRemoteAddress()).isEqualTo(channel.remoteAddress()).isEqualTo(InetSocketAddress.createUnresolved("localhost", 1234));

    session.close();

    verify(channel).writeAndFlush(new CloseWebSocketFrame(CloseStatus.NORMAL.getCode(), CloseStatus.NORMAL.getReason()));
    Thread.sleep(1000);
    verify(channel).close();
  }

  @Test
  void send() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);

    given(channel.writeAndFlush(any())).willReturn(promise);

    promise.setSuccess();

    session.sendText("text");
    session.sendText("");

    session.sendPing();
    session.sendPong();

    session.sendBinary(DataBuffer.empty());
    session.sendBinary(factory -> factory.copiedBuffer("demo"));
    session.send(session.textMessage("send"));
    session.send(Future.ok(session.textMessage("Future")));
    session.send(session.binaryMessage(factory -> factory.copiedBuffer("binaryMessage")));
    session.send(WebSocketMessage.binary(session.bufferFactory().copiedBuffer("send")));
    session.send(WebSocketMessage.ping(session.bufferFactory().copiedBuffer("ping")));
    session.send(WebSocketMessage.pong(session.bufferFactory().copiedBuffer("pong")));

    TextWebSocketFrame nativeMessage = new TextWebSocketFrame(Unpooled.copiedBuffer("nativeMessage".getBytes()));
    session.send(new WebSocketMessage(WebSocketMessage.Type.TEXT, DataBuffer.empty(),
            nativeMessage, true));

    verify(channel).writeAndFlush(new TextWebSocketFrame("text"));
    verify(channel).writeAndFlush(new TextWebSocketFrame(Unpooled.EMPTY_BUFFER));
    verify(channel).writeAndFlush(new PingWebSocketFrame());
    verify(channel).writeAndFlush(new PongWebSocketFrame());
    verify(channel).writeAndFlush(new BinaryWebSocketFrame(Unpooled.EMPTY_BUFFER));
    verify(channel).writeAndFlush(new TextWebSocketFrame("Future"));
    verify(channel).writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer("binaryMessage".getBytes())));
    verify(channel).writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer("demo".getBytes())));
    verify(channel).writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer("send".getBytes())));
    verify(channel).writeAndFlush(new TextWebSocketFrame("send"));
    verify(channel).writeAndFlush(new PingWebSocketFrame(Unpooled.copiedBuffer("ping".getBytes())));
    verify(channel).writeAndFlush(new PongWebSocketFrame(Unpooled.copiedBuffer("pong".getBytes())));
    verify(channel).writeAndFlush(nativeMessage);
  }

  @Test
  void constructorInitializesFields() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    String acceptedProtocol = "test-protocol";

    NettyWebSocketSession session = new NettyWebSocketSession(true, channel, allocator, acceptedProtocol);

    assertThat(session.isSecure()).isTrue();
    assertThat(session.bufferFactory()).isSameAs(allocator);
    assertThat(session.getAcceptedProtocol()).isEqualTo(acceptedProtocol);
  }

  @Test
  void constructorWithNullProtocol() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    assertThat(session.isSecure()).isFalse();
    assertThat(session.getAcceptedProtocol()).isNull();
  }

  @Test
  void sendTextWithNonEmptyText() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    Future<Void> result = session.sendText("Hello World");

    verify(channel).writeAndFlush(new TextWebSocketFrame("Hello World"));
    assertThat(result).isNotNull();
  }

  @Test
  void sendTextWithEmptyText() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    Future<Void> result = session.sendText("");

    verify(channel).writeAndFlush(new TextWebSocketFrame(Unpooled.EMPTY_BUFFER));
    assertThat(result).isNotNull();
  }

  @Test
  void sendBinaryWithDataBuffer() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    DataBuffer dataBuffer = allocator.allocateBuffer(10);
    Future<Void> result = session.sendBinary(dataBuffer);

    verify(channel).writeAndFlush(any(BinaryWebSocketFrame.class));
    assertThat(result).isNotNull();
  }

  @Test
  void sendPing() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    Future<Void> result = session.sendPing();

    verify(channel).writeAndFlush(new PingWebSocketFrame());
    assertThat(result).isNotNull();
  }

  @Test
  void sendPong() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    Future<Void> result = session.sendPong();

    verify(channel).writeAndFlush(new PongWebSocketFrame());
    assertThat(result).isNotNull();
  }

  @Test
  void sendWebSocketMessage() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    WebSocketMessage message = session.textMessage("test");
    Future<Void> result = session.send(message);

    verify(channel).writeAndFlush(any(TextWebSocketFrame.class));
    assertThat(result).isNotNull();
  }

  @Test
  void createFrameWithNativeMessage() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    TextWebSocketFrame nativeFrame = new TextWebSocketFrame("native");
    WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.TEXT, DataBuffer.empty(), nativeFrame, true);

    WebSocketFrame result = session.createFrame(message);

    assertThat(result).isSameAs(nativeFrame);
  }

  @Test
  void createFrameWithTextMessage() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    DataBuffer payload = allocator.copiedBuffer("text");
    WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.TEXT, payload, null, true);

    WebSocketFrame result = session.createFrame(message);

    assertThat(result).isInstanceOf(TextWebSocketFrame.class);
    TextWebSocketFrame textFrame = (TextWebSocketFrame) result;
    assertThat(textFrame.text()).isEqualTo("text");
  }

  @Test
  void createFrameWithBinaryMessage() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    byte[] data = "binary".getBytes();
    DataBuffer payload = allocator.wrap(data);
    WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY, payload, null, true);

    WebSocketFrame result = session.createFrame(message);

    assertThat(result).isInstanceOf(BinaryWebSocketFrame.class);
    BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) result;
    assertThat(binaryFrame.content().array()).isEqualTo(data);
  }

  @Test
  void createFrameWithPingMessage() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    DataBuffer payload = allocator.copiedBuffer("ping");
    WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.PING, payload, null, true);

    WebSocketFrame result = session.createFrame(message);

    assertThat(result).isInstanceOf(PingWebSocketFrame.class);
  }

  @Test
  void createFrameWithPongMessage() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    DataBuffer payload = allocator.copiedBuffer("pong");
    WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.PONG, payload, null, true);

    WebSocketFrame result = session.createFrame(message);

    assertThat(result).isInstanceOf(PongWebSocketFrame.class);
  }

  @Test
  void abortClosesChannel() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    session.abort();

    verify(channel).close();
  }

  @Test
  void closeWithStatus() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    CloseStatus status = CloseStatus.GOING_AWAY;
    Future<Void> result = session.close(status);

    verify(channel).writeAndFlush(new CloseWebSocketFrame(status.getCode(), status.getReason()));
    assertThat(result).isNotNull();
  }

  @Test
  void getRemoteAddress() {
    Channel channel = mock(Channel.class);
    InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("example.com", 8080);
    given(channel.remoteAddress()).willReturn(remoteAddress);

    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    InetSocketAddress result = session.getRemoteAddress();

    assertThat(result).isSameAs(remoteAddress);
  }

  @Test
  void getLocalAddress() {
    Channel channel = mock(Channel.class);
    InetSocketAddress localAddress = InetSocketAddress.createUnresolved("localhost", 9090);
    given(channel.localAddress()).willReturn(localAddress);

    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    InetSocketAddress result = session.getLocalAddress();

    assertThat(result).isSameAs(localAddress);
  }

  @Test
  void equalsAndHashCode() {
    Channel channel1 = mock(Channel.class);
    Channel channel2 = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    NettyWebSocketSession session1 = new NettyWebSocketSession(true, channel1, allocator, null);
    NettyWebSocketSession session2 = new NettyWebSocketSession(true, channel1, allocator, null);
    NettyWebSocketSession session3 = new NettyWebSocketSession(false, channel1, allocator, null);
    NettyWebSocketSession session4 = new NettyWebSocketSession(true, channel2, allocator, null);

    assertThat(session1).isEqualTo(session2);
    assertThat(session1).hasSameHashCodeAs(session2);

    assertThat(session1).isNotEqualTo(session3);
    assertThat(session1.hashCode()).isNotEqualTo(session3.hashCode());

    assertThat(session1).isNotEqualTo(session4);
    assertThat(session1.hashCode()).isNotEqualTo(session4.hashCode());
  }

  @Test
  void toStringProvidesMeaningfulOutput() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(true, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), "test-protocol");

    String result = session.toString();

    assertThat(result).contains("NettyWebSocketSession");
    assertThat(result).contains("channel=");
    assertThat(result).contains("secure=true");
  }

  @Test
  void onCloseWithNormalStatus() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    CloseStatus closeStatus = CloseStatus.NORMAL;
    Logger logger = mock(Logger.class);

    given(channel.isActive()).willReturn(true);
    given(handler.onClose(session, closeStatus)).willReturn(null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    session.onClose(handler, closeStatus, logger);

    verify(channel).writeAndFlush(new CloseWebSocketFrame(closeStatus.getCode(), closeStatus.getReason()));
  }

  @Test
  void onCloseWithNoStatusCodeUsesNormal() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    CloseStatus closeStatus = CloseStatus.NO_STATUS_CODE;
    Logger logger = mock(Logger.class);

    given(channel.isActive()).willReturn(true);
    given(handler.onClose(session, closeStatus)).willReturn(null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    session.onClose(handler, closeStatus, logger);

    verify(channel).writeAndFlush(new CloseWebSocketFrame(CloseStatus.NORMAL.getCode(), CloseStatus.NORMAL.getReason()));
  }

  @Test
  void onCloseWithNoCloseFrameUsesNormal() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    CloseStatus closeStatus = CloseStatus.NO_CLOSE_FRAME;
    Logger logger = mock(Logger.class);

    given(channel.isActive()).willReturn(true);
    given(handler.onClose(session, closeStatus)).willReturn(null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    session.onClose(handler, closeStatus, logger);

    verify(channel).writeAndFlush(new CloseWebSocketFrame(CloseStatus.NORMAL.getCode(), CloseStatus.NORMAL.getReason()));
  }

  @Test
  void onCloseWithFutureReturned() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    CloseStatus closeStatus = CloseStatus.NORMAL;
    Logger logger = mock(Logger.class);
    Future<Void> handlerFuture = Future.ok(null);

    given(channel.isActive()).willReturn(true);
    given(handler.onClose(session, closeStatus)).willReturn(handlerFuture);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    promise.setSuccess();

    session.onClose(handler, closeStatus, logger);

    verify(channel).isActive();
    verify(channel).writeAndFlush(new CloseWebSocketFrame(closeStatus.getCode(), closeStatus.getReason()));
  }

  @Test
  void onCloseWhenChannelNotActive() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    CloseStatus closeStatus = CloseStatus.NORMAL;
    Logger logger = mock(Logger.class);

    given(channel.isActive()).willReturn(false);

    session.onClose(handler, closeStatus, logger);

    verify(channel, never()).writeAndFlush(any());
  }

  @Test
  void onCloseWithExceptionInHandler() {
    Channel channel = mock(Channel.class);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel,
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    CloseStatus closeStatus = CloseStatus.NORMAL;
    Logger logger = mock(Logger.class);

    given(channel.isActive()).willReturn(true);
    given(handler.onClose(session, closeStatus)).willThrow(new RuntimeException("Test exception"));

    session.onClose(handler, closeStatus, logger);

    verify(logger).warn(anyString(), any(NettyWebSocketSession.class), any(RuntimeException.class));
  }

  @Test
  void handleMessageWithTextFrame() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    TextWebSocketFrame frame = new TextWebSocketFrame("test message");
    Logger logger = mock(Logger.class);

    given(handler.handleMessage(eq(session), any(WebSocketMessage.class))).willReturn(null);

    session.handleMessage(handler, frame, logger);

    verify(handler).handleMessage(eq(session), any(WebSocketMessage.class));
  }

  @Test
  void handleMessageWithBinaryFrame() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.copiedBuffer("binary data".getBytes()));
    Logger logger = mock(Logger.class);

    given(handler.handleMessage(eq(session), any(WebSocketMessage.class))).willReturn(null);

    session.handleMessage(handler, frame, logger);

    verify(handler).handleMessage(eq(session), any(WebSocketMessage.class));
  }

  @Test
  void handleMessageWithPingFrame() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    PingWebSocketFrame frame = new PingWebSocketFrame(Unpooled.copiedBuffer("ping data".getBytes()));
    Logger logger = mock(Logger.class);

    given(handler.handleMessage(eq(session), any(WebSocketMessage.class))).willReturn(null);

    session.handleMessage(handler, frame, logger);

    verify(handler).handleMessage(eq(session), any(WebSocketMessage.class));
  }

  @Test
  void handleMessageWithPongFrame() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    PongWebSocketFrame frame = new PongWebSocketFrame(Unpooled.copiedBuffer("pong data".getBytes()));
    Logger logger = mock(Logger.class);

    given(handler.handleMessage(eq(session), any(WebSocketMessage.class))).willReturn(null);

    session.handleMessage(handler, frame, logger);

    verify(handler).handleMessage(eq(session), any(WebSocketMessage.class));
  }

  @Test
  void handleMessageWithExceptionInHandler() {
    Channel channel = mock(Channel.class);
    NettyDataBufferFactory allocator = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    NettyWebSocketSession session = new NettyWebSocketSession(false, channel, allocator, null);

    WebSocketHandler handler = mock(WebSocketHandler.class);
    TextWebSocketFrame frame = new TextWebSocketFrame("test message");

    Logger logger = mock(Logger.class);

    given(logger.isErrorEnabled()).willReturn(true);

    given(handler.handleMessage(eq(session), any(WebSocketMessage.class)))
            .willThrow(new RuntimeException("Handler exception"));

    session.handleMessage(handler, frame, logger);

    verify(logger).error(anyString(), any(NettyWebSocketSession.class), any(RuntimeException.class));
  }

}