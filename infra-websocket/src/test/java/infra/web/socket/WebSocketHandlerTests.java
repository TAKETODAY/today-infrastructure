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

package infra.web.socket;

import org.junit.jupiter.api.Test;

import infra.core.io.buffer.DataBuffer;
import infra.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketHandlerTests {

  private final WebSocketSession session = mock(WebSocketSession.class);

  @Test
  void defaultHandlerHandlesAllMessageTypes() throws Exception {
    ExposedHandler handler = new ExposedHandler();

    assertThat(handler.handleMessage(session, WebSocketMessage.ping())).isNull();
    assertThat(handler.handleMessage(session, WebSocketMessage.pong())).isNull();
    assertThat(handler.handleMessage(session, WebSocketMessage.text(DataBuffer.empty()))).isNull();
    assertThat(handler.handleMessage(session, WebSocketMessage.binary(DataBuffer.empty()))).isNull();
    assertThat(handler.onClose(session, CloseStatus.NORMAL)).isNull();
    handler.onOpen(session);
    handler.onError(session, new IllegalStateException("error"));
  }

  @Test
  void delegatesLifecycleAndMessages() throws Exception {
    WebSocketHandler delegate = mock(WebSocketHandler.class);
    WebSocketHandler handler = new ExposedHandler(delegate);
    WebSocketMessage message = WebSocketMessage.text(DataBuffer.empty());
    Throwable failure = new IllegalStateException("error");

    assertThat(handler.getRawHandler()).isSameAs(delegate);
    handler.onOpen(session);
    handler.handleMessage(session, message);
    handler.onClose(session, CloseStatus.NORMAL);
    handler.onError(session, failure);

    verify(delegate).onOpen(session);
    verify(delegate).handleMessage(session, message);
    verify(delegate).onClose(session, CloseStatus.NORMAL);
    verify(delegate).onError(session, failure);
  }

  @Test
  void delegatesProtectedHandlers() {
    WebSocketHandler delegate = mock(WebSocketHandler.class);
    ExposedHandler handler = new ExposedHandler(delegate);
    WebSocketMessage message = WebSocketMessage.binary(DataBuffer.empty());

    assertThat(handler.callPing(session, message)).isNull();
    assertThat(handler.callPong(session, message)).isNull();
    assertThat(handler.callText(session, message)).isNull();
    assertThat(handler.callBinary(session, message)).isNull();

    verify(delegate).handlePingMessage(session, message);
    verify(delegate).handlePongMessage(session, message);
    verify(delegate).handleTextMessage(session, message);
    verify(delegate).handleBinaryMessage(session, message);
  }

  private static class ExposedHandler extends WebSocketHandler {

    ExposedHandler() {
    }

    ExposedHandler(WebSocketHandler delegate) {
      super(delegate);
    }

    Future<Void> callPing(WebSocketSession session, WebSocketMessage message) {
      return handlePingMessage(session, message);
    }

    Future<Void> callPong(WebSocketSession session, WebSocketMessage message) {
      return handlePongMessage(session, message);
    }

    Future<Void> callText(WebSocketSession session, WebSocketMessage message) {
      return handleTextMessage(session, message);
    }

    Future<Void> callBinary(WebSocketSession session, WebSocketMessage message) {
      return handleBinaryMessage(session, message);
    }
  }

}
