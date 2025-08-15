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
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
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
import io.netty.util.concurrent.GlobalEventExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/14 21:58
 */
class NettyWebSocketSessionTests {

  @Test
  void close() {
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

}