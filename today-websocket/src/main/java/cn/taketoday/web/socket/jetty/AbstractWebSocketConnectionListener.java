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

package cn.taketoday.web.socket.jetty;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketConnectionListener;
import org.eclipse.jetty.websocket.api.WebSocketPingPongListener;

import java.nio.ByteBuffer;

import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.WebSocketHandler;

/**
 * @author TODAY 2021/5/6 23:13
 * @since 3.0.1
 */
public abstract class AbstractWebSocketConnectionListener
        implements WebSocketConnectionListener, WebSocketPingPongListener {

  protected final WebSocketHandler handler;
  protected final JettyWebSocketSession session;

  public AbstractWebSocketConnectionListener(JettyWebSocketSession session, WebSocketHandler handler) {
    this.session = session;
    this.handler = handler;
  }

  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    handler.onClose(session, new CloseStatus(statusCode, reason));
  }

  @Override
  public void onWebSocketConnect(Session session) {
    this.session.initializeNativeSession(session);
  }

  @Override
  public void onWebSocketError(Throwable cause) {
    handler.onError(session, cause);
  }

  // WebSocketPingPongListener

  @Override
  public void onWebSocketPing(ByteBuffer payload) {
    handler.handleMessage(session, new PingMessage(payload));
  }

  @Override
  public void onWebSocketPong(ByteBuffer payload) {
    handler.handleMessage(session, new PongMessage(payload));
  }

  public final JettyWebSocketSession getSession() {
    return session;
  }
}
