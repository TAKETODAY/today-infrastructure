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

import infra.web.socket.WebSocketHandler;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/28 17:32
 */
final class WebSocketAttribute {

  private static final AttributeKey<@Nullable WebSocketAttribute> KEY = AttributeKey.valueOf(WebSocketAttribute.class, "KEY");

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
