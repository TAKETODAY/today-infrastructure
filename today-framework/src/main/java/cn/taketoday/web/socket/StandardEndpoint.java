/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

/**
 * Websocket Main Endpoint
 *
 * @author TODAY 2021/5/3 22:56
 * @since 3.0.1
 */
public class StandardEndpoint extends Endpoint {
  private final WebSocketHandler socketHandler;
  private final StandardWebSocketSession session;

  public StandardEndpoint(StandardWebSocketSession session, WebSocketHandler handler) {
    this.session = session;
    this.socketHandler = handler;
  }

  @Override
  public void onOpen(Session stdSession, EndpointConfig config) {
    session.initializeNativeSession(stdSession);

    WebSocketHandler socketHandler = this.socketHandler;
    socketHandler.onOpen(session);

    if (socketHandler.supportPartialMessage()) {
      final class StringPartialHandler implements MessageHandler.Partial<String> {
        @Override
        public void onMessage(final String partialMessage, final boolean last) {
          final TextMessage textMessage = new TextMessage(partialMessage, last);
          socketHandler.handleMessage(session, textMessage);
        }
      }
      final class ByteBufferPartialHandler implements MessageHandler.Partial<ByteBuffer> {
        @Override
        public void onMessage(final ByteBuffer partialMessage, final boolean last) {
          final BinaryMessage textMessage = new BinaryMessage(partialMessage, last);
          socketHandler.handleMessage(session, textMessage);
        }
      }
      stdSession.addMessageHandler(new StringPartialHandler());
      stdSession.addMessageHandler(new ByteBufferPartialHandler());
    }
    else {
      final class StringWholeHandler implements MessageHandler.Whole<String> {
        @Override
        public void onMessage(final String message) {
          final TextMessage textMessage = new TextMessage(message);
          socketHandler.handleMessage(session, textMessage);
        }
      }
      final class ByteBufferWholeHandler implements MessageHandler.Whole<ByteBuffer> {
        @Override
        public void onMessage(final ByteBuffer message) {
          final BinaryMessage textMessage = new BinaryMessage(message);
          socketHandler.handleMessage(session, textMessage);
        }
      }

      stdSession.addMessageHandler(new StringWholeHandler());
      stdSession.addMessageHandler(new ByteBufferWholeHandler());
    }

    // TODO PING PONG
  }

  @Override
  public void onClose(Session stdSession, CloseReason closeReason) {
    socketHandler.onClose(session);
  }

  @Override
  public void onError(Session stdSession, Throwable thr) {
    socketHandler.onError(session, thr);
  }
}
