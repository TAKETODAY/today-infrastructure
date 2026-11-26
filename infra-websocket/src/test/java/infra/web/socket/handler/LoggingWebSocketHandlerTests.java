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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import infra.util.concurrent.Future;
import infra.web.ServerHttpExchange;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import infra.web.socket.server.HandshakeCapable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoggingWebSocketHandlerTests {

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void delegatesCallbacksAtBothLogLevels(boolean trace) throws Exception {
    HandshakeHandler delegate = mock(HandshakeHandler.class);
    LoggingWebSocketHandler handler = new LoggingWebSocketHandler(delegate);
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    WebSocketSession session = mock(WebSocketSession.class);
    WebSocketMessage message = WebSocketMessage.ping();
    Map<String, Object> attributes = Map.of("test", "value");
    Throwable failure = new IllegalStateException("test failure");
    Future<Void> result = Future.ok();
    given(delegate.handleMessage(session, message)).willReturn(result);
    Logger logger = (Logger) LoggerFactory.getLogger(LoggingWebSocketHandler.class);
    Level previous = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(trace ? Level.TRACE : Level.OFF);
    try {
      assertThat(handler.beforeHandshake(context, attributes)).isFalse();
      handler.afterHandshake(context, session, null);
      handler.afterHandshake(context, null, failure);
      handler.onOpen(session);
      assertThat(handler.handleMessage(session, message)).isSameAs(result);
      handler.onError(session, failure);
      handler.onClose(session, CloseStatus.NORMAL);

      verify(delegate).beforeHandshake(context, attributes);
      verify(delegate).afterHandshake(context, session, null);
      verify(delegate).afterHandshake(context, null, failure);
      verify(delegate).onOpen(session);
      verify(delegate).onError(session, failure);
      verify(delegate).onClose(session, CloseStatus.NORMAL);
      if (trace) {
        assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.DEBUG, Level.DEBUG, Level.ERROR, Level.DEBUG, Level.TRACE, Level.DEBUG, Level.DEBUG);
      }
      else {
        assertThat(appender.list).isEmpty();
      }
    }
    finally {
      logger.detachAppender(appender);
      logger.setLevel(previous);
      appender.stop();
    }
  }

  @Test
  void acceptsHandshakeWhenDelegateHasNoHandshakeCallbacks() throws Exception {
    LoggingWebSocketHandler handler = new LoggingWebSocketHandler(mock(WebSocketHandler.class));
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    assertThat(handler.beforeHandshake(context, Map.of())).isTrue();
    handler.afterHandshake(context, mock(WebSocketSession.class), null);
  }

  private abstract static class HandshakeHandler extends WebSocketHandler implements HandshakeCapable {
  }

}
