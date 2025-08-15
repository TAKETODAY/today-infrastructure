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

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.web.socket.CloseStatus;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
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
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT),null);

    DefaultChannelPromise promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
    given(channel.writeAndFlush(any())).willReturn(promise);
    given(channel.isActive()).willReturn(true);
    given(channel.isOpen()).willReturn(true);

    promise.setSuccess();

    assertThat(session.isSecure()).isFalse();
    assertThat(session.isOpen()).isTrue();
    assertThat(session.isActive()).isTrue();

    session.close();

    verify(channel).writeAndFlush(new CloseWebSocketFrame(CloseStatus.NORMAL.getCode(), CloseStatus.NORMAL.getReason()));
    verify(channel).close();
  }

  @Test
  void f() {

  }

}