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

package infra.web.socket.handler;

import org.jspecify.annotations.Nullable;

import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;

/**
 * A convenient base class for {@link WebSocketHandler} implementations
 * that process binary messages only.
 *
 * <p>Text messages are rejected with {@link CloseStatus#NOT_ACCEPTABLE}.
 * All other methods have empty implementations.
 *
 * @author TODAY 2021/5/6 18:16
 * @since 3.0.1
 */
public class BinaryWebSocketHandler extends WebSocketHandler {

  public BinaryWebSocketHandler() {
    this(null);
  }

  public BinaryWebSocketHandler(@Nullable WebSocketHandler delegate) {
    super(delegate);
  }

  @Nullable
  @Override
  protected final Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
    return null;
  }

}
