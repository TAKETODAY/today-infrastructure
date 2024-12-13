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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.web.socket.CloseStatus;
import infra.web.socket.BinaryMessage;
import infra.web.socket.TextMessage;
import infra.web.socket.WebSocketSession;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/16 10:27
 */
class BinaryWebSocketHandlerTests {

  private final BinaryWebSocketHandler handler = new BinaryWebSocketHandler();

  private final WebSocketSession session = mock(WebSocketSession.class);

  @Test
  void handleTextMessage() throws Exception {
    handler.handleMessage(session, new BinaryMessage("hello".getBytes(StandardCharsets.UTF_8)));
    handler.handleMessage(session, new TextMessage("hello"));

    verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
  }

  @Test
  void ioEx() throws Exception {
    doThrow(IOException.class).when(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
    handler.handleMessage(session, new TextMessage("hello"));
    verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
  }

}