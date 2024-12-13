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

package infra.web.socket.handler;

import java.io.IOException;

import infra.lang.Nullable;
import infra.web.socket.CloseStatus;
import infra.web.socket.BinaryMessage;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;

/**
 * A convenient base class for {@link WebSocketHandler} implementations
 * that process text messages only.
 *
 * <p>Binary messages are rejected with {@link CloseStatus#NOT_ACCEPTABLE}.
 * All other methods have empty implementations.
 *
 * @author TODAY 2021/5/6 18:07
 * @since 3.0.1
 */
public class TextWebSocketHandler extends WebSocketHandler {

  public TextWebSocketHandler() {
    this(null);
  }

  public TextWebSocketHandler(@Nullable WebSocketHandler delegate) {
    super(delegate);
  }

  @Override
  protected final void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    try {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
    }
    catch (IOException ex) {
      // ignore
    }
  }

}
