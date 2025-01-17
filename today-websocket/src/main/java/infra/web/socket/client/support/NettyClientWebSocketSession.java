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

package infra.web.socket.client.support;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Nullable;
import infra.web.socket.server.support.NettyWebSocketSession;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

/**
 * Netty client websocket session
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/3 10:42
 */
final class NettyClientWebSocketSession extends NettyWebSocketSession {

  @Nullable
  private final String acceptedProtocol;

  NettyClientWebSocketSession(boolean secure, Channel channel,
          WebSocketClientHandshaker handshaker, NettyDataBufferFactory allocator) {
    super(secure, channel, allocator);
    this.acceptedProtocol = handshaker.actualSubprotocol();
  }

  @Override
  @Nullable
  public String getAcceptedProtocol() {
    return acceptedProtocol;
  }

}
