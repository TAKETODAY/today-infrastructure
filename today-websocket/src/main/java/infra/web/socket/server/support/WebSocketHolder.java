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

package infra.web.socket.server.support;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.lang.Nullable;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/28 17:32
 */
final class WebSocketHolder {

  private static final AttributeKey<WebSocketHolder> KEY = AttributeKey.valueOf(WebSocketHolder.class, "KEY");

  public final WebSocketHandler wsHandler;

  public final WebSocketSession session;

  /**
   * @since 5.0
   */
  public final NettyDataBufferFactory allocator;

  private WebSocketHolder(WebSocketHandler wsHandler, WebSocketSession session, ByteBufAllocator allocator) {
    this.wsHandler = wsHandler;
    this.session = session;
    this.allocator = new NettyDataBufferFactory(allocator);
  }

  public void unbind(AttributeMap attributes) {
    attributes.attr(KEY).set(null);
  }

  public static void bind(Channel channel, WebSocketHandler wsHandler, WebSocketSession session) {
    channel.attr(KEY).set(new WebSocketHolder(wsHandler, session, channel.alloc()));
  }

  @Nullable
  public static WebSocketHolder find(AttributeMap attributes) {
    return attributes.attr(KEY).get();
  }

}
