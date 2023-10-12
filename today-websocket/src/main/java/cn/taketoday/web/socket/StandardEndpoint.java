/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket;

import java.nio.ByteBuffer;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import static cn.taketoday.web.socket.handler.WebSocketSessionDecorator.unwrapRequired;

/**
 * Websocket Main Endpoint
 *
 * @author TODAY 2021/5/3 22:56
 * @since 3.0.1
 */
public class StandardEndpoint extends Endpoint {
  private static final Logger logger = LoggerFactory.getLogger(StandardEndpoint.class);

  private final WebSocketHandler socketHandler;

  private final WebSocketSession session;

  public StandardEndpoint(WebSocketSession session, WebSocketHandler handler) {
    this.session = session;
    this.socketHandler = handler;
  }

  @Override
  public void onOpen(Session stdSession, EndpointConfig config) {
    unwrapRequired(session, StandardWebSocketSession.class)
            .initializeNativeSession(stdSession);

    // The following inner classes need to remain since lambdas would not retain their
    // declared generic types (which need to be seen by the underlying WebSocket engine)

    if (socketHandler.supportPartialMessage()) {
      stdSession.addMessageHandler(new MessageHandler.Partial<String>() {
        @Override
        public void onMessage(String partialMessage, boolean last) {
          handleMessage(new TextMessage(partialMessage, last));
        }
      });
      stdSession.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
        @Override
        public void onMessage(ByteBuffer partialMessage, boolean last) {
          handleMessage(new BinaryMessage(partialMessage, last));
        }
      });
    }
    else {
      stdSession.addMessageHandler(new MessageHandler.Whole<String>() {
        @Override
        public void onMessage(String message) {
          handleMessage(new TextMessage(message));
        }
      });
      stdSession.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
        @Override
        public void onMessage(ByteBuffer message) {
          handleMessage(new BinaryMessage(message));
        }
      });
    }

    stdSession.addMessageHandler(new MessageHandler.Whole<jakarta.websocket.PongMessage>() {
      @Override
      public void onMessage(jakarta.websocket.PongMessage message) {
        handleMessage(new PongMessage(message.getApplicationData()));
      }
    });

    try {
      socketHandler.onOpen(session);
    }
    catch (Exception e) {
      ExceptionWebSocketHandlerDecorator.tryCloseWithError(session, e, logger);
    }
  }

  private void handleMessage(Message<?> message) {
    try {
      socketHandler.handleMessage(session, message);
    }
    catch (Exception e) {
      ExceptionWebSocketHandlerDecorator.tryCloseWithError(session, e, logger);
    }
  }

  @Override
  public void onClose(Session stdSession, CloseReason closeReason) {
    try {
      socketHandler.onClose(session);
    }
    catch (Exception ex) {
      logger.warn("Unhandled on-close exception for {}", session, ex);
    }
  }

  @Override
  public void onError(Session stdSession, Throwable thr) {
    try {
      socketHandler.onError(session, thr);
    }
    catch (Exception ex) {
      ExceptionWebSocketHandlerDecorator.tryCloseWithError(session, ex, logger);
    }
  }
}
