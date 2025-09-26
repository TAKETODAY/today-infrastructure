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

import org.jspecify.annotations.Nullable;

import infra.web.socket.WebSocketHandler;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/28 17:32
 */
final class WebSocketAttribute {

  private static final AttributeKey<WebSocketAttribute> KEY = AttributeKey.valueOf(WebSocketAttribute.class, "KEY");

  public final WebSocketHandler wsHandler;

  public final NettyWebSocketSession session;

  private WebSocketAttribute(WebSocketHandler wsHandler, NettyWebSocketSession session) {
    this.wsHandler = wsHandler;
    this.session = session;
  }

  public void unbind(AttributeMap attributes) {
    attributes.attr(KEY).set(null);
  }

  public static void bind(Channel channel, WebSocketHandler wsHandler, NettyWebSocketSession session) {
    channel.attr(KEY).set(new WebSocketAttribute(wsHandler, session));
  }

  @Nullable
  public static WebSocketAttribute find(AttributeMap attributes) {
    return attributes.attr(KEY).get();
  }

}
