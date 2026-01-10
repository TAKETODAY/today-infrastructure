/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket.handler;

import org.junit.jupiter.api.Test;

import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/7 21:50
 */
class ExceptionWebSocketHandlerDecoratorTests {

  private TestWebSocketSession session = new TestWebSocketSession(true);

  private WebSocketHandler delegate = mock();

  private ExceptionWebSocketHandlerDecorator decorator = new ExceptionWebSocketHandlerDecorator(this.delegate);

  @Test
  void afterConnectionEstablished() throws Throwable {
    willThrow(new IllegalStateException("error"))
            .given(this.delegate).onOpen(this.session);

    this.decorator.onOpen(this.session);

    assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
  }

  @Test
  void handleMessage() throws Throwable {
    WebSocketMessage message = session.textMessage("payload");

    willThrow(new IllegalStateException("error"))
            .given(this.delegate).handleMessage(this.session, message);

    this.decorator.handleMessage(this.session, message);

    assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
  }

  @Test
  void handleTransportError() throws Throwable {
    Exception exception = new Exception("transport error");

    willThrow(new IllegalStateException("error"))
            .given(this.delegate).onError(this.session, exception);

    this.decorator.onError(this.session, exception);

    assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
  }

  @Test
  void afterConnectionClosed() throws Throwable {
    CloseStatus closeStatus = CloseStatus.NORMAL;

    willThrow(new IllegalStateException("error"))
            .given(this.delegate).onClose(this.session, closeStatus);

    this.decorator.onClose(this.session, closeStatus);

    assertThat(this.session.getCloseStatus()).isNull();
  }

}
