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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/16 10:27
 */
class BinaryWebSocketHandlerTests {

  private final BinaryWebSocketHandler handler = new BinaryWebSocketHandler();

  private final WebSocketSession session = mock(WebSocketSession.class);

  private final DataBuffer hello = DefaultDataBufferFactory.sharedInstance.copiedBuffer("hello", StandardCharsets.UTF_8);

  @Test
  void handleTextMessage() throws Throwable {
    handler.handleMessage(session, WebSocketMessage.binary(hello));
    handler.handleMessage(session, WebSocketMessage.text(hello));

    verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
  }

  @Test
  void ioEx() throws Throwable {
    handler.handleMessage(session, WebSocketMessage.text(hello));
    verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
  }

}