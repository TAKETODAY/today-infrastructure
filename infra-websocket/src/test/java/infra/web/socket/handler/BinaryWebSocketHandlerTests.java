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
