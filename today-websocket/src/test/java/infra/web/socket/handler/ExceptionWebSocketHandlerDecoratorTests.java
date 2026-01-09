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
