/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.socket.jetty;

import org.eclipse.jetty.websocket.api.Frame;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.core.OpCode;

import java.nio.ByteBuffer;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * Adapts {@link WebSocketHandler} to the Jetty 9 WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/7 16:39
 * @since 4.0
 */
@WebSocket
public class JettyWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(JettyWebSocketHandler.class);
  private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.wrap(Constant.EMPTY_BYTES);

  private final JettyWebSocketSession wsSession;
  private final WebSocketHandler webSocketHandler;

  public JettyWebSocketHandler(WebSocketHandler webSocketHandler, JettyWebSocketSession wsSession) {
    Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
    Assert.notNull(wsSession, "WebSocketSession must not be null");
    this.webSocketHandler = webSocketHandler;
    this.wsSession = wsSession;
  }

  @OnWebSocketConnect
  public void onWebSocketConnect(Session session) {
    try {
      this.wsSession.initializeNativeSession(session);
      this.webSocketHandler.onOpen(this.wsSession);
    }
    catch (Exception ex) {
      tryCloseWithError(this.wsSession, ex);
    }
  }

  @OnWebSocketMessage
  public void onWebSocketText(String payload) {
    TextMessage message = new TextMessage(payload);
    try {
      this.webSocketHandler.handleMessage(this.wsSession, message);
    }
    catch (Exception ex) {
      tryCloseWithError(this.wsSession, ex);
    }
  }

  @OnWebSocketMessage
  public void onWebSocketBinary(byte[] payload, int offset, int length) {
    BinaryMessage message = new BinaryMessage(payload, offset, length, true);
    try {
      this.webSocketHandler.handleMessage(this.wsSession, message);
    }
    catch (Exception ex) {
      tryCloseWithError(this.wsSession, ex);
    }
  }

  @OnWebSocketFrame
  public void onWebSocketFrame(Frame frame) {
    if (OpCode.PONG == frame.getOpCode()) {
      ByteBuffer payload = frame.getPayload() != null ? frame.getPayload() : EMPTY_PAYLOAD;
      PongMessage message = new PongMessage(payload);
      try {
        this.webSocketHandler.handleMessage(this.wsSession, message);
      }
      catch (Exception ex) {
        tryCloseWithError(this.wsSession, ex);
      }
    }
  }

  @OnWebSocketClose
  public void onWebSocketClose(int statusCode, String reason) {
    CloseStatus closeStatus = new CloseStatus(statusCode, reason);
    try {
      this.webSocketHandler.onClose(this.wsSession, closeStatus);
    }
    catch (Exception ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Unhandled exception after connection closed for " + this, ex);
      }
    }
  }

  @OnWebSocketError
  public void onWebSocketError(Throwable cause) {
    try {
      this.webSocketHandler.onError(this.wsSession, cause);
    }
    catch (Exception ex) {
      tryCloseWithError(this.wsSession, ex);
    }
  }

  public static void tryCloseWithError(WebSocketSession session, Throwable exception) {
    if (logger.isErrorEnabled()) {
      logger.error("Closing session due to exception for {}", session, exception);
    }
    if (session.isOpen()) {
      try {
        session.close(CloseStatus.SERVER_ERROR);
      }
      catch (Throwable ex) {
        // ignore
      }
    }
  }

}
