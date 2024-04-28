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

package cn.taketoday.web.socket.server.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
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

  private WebSocketHolder(WebSocketHandler wsHandler, WebSocketSession session) {
    this.wsHandler = wsHandler;
    this.session = session;
  }

  public void unbind(AttributeMap attributes) {
    attributes.attr(KEY).set(null);
  }

  public static void bind(AttributeMap attributes, WebSocketHandler wsHandler, WebSocketSession session) {
    attributes.attr(KEY).set(new WebSocketHolder(wsHandler, session));
  }

  @Nullable
  public static WebSocketHolder find(AttributeMap attributes) {
    return attributes.attr(KEY).get();
  }

}
