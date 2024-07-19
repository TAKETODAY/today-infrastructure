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

package cn.taketoday.web.socket.handler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketSession;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/16 11:06
 */
class TextWebSocketHandlerTests {

  private final TextWebSocketHandler handler = new TextWebSocketHandler();

  private final WebSocketSession session = mock(WebSocketSession.class);

  @Test
  void handleTextMessage() throws Exception {
    handler.handleMessage(session, new TextMessage("hello"));
    handler.handleMessage(session, new BinaryMessage("hello".getBytes(StandardCharsets.UTF_8)));

    verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
  }

  @Test
  void ioEx() throws Exception {
    doThrow(IOException.class).when(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
    handler.handleMessage(session, new BinaryMessage("hello".getBytes(StandardCharsets.UTF_8)));

    verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
  }

}