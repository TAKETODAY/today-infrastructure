/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.IOException;
import java.nio.ByteBuffer;

import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * @author TODAY 2021/5/6 21:40
 * @since 3.0.1
 */
public class JettySession extends WebSocketSession {
  private final Session session;

  public JettySession(Session session) {
    this.session = session;
  }

  @Override
  public void sendText(String text) throws IOException {
    session.getRemote().sendString(text);
  }

  @Override
  public void sendPartialText(String partialMessage, boolean isLast) throws IOException {
    session.getRemote().sendPartialString(partialMessage, isLast);
  }

  @Override
  public void sendBinary(BinaryMessage data) throws IOException {
    session.getRemote().sendBytes(data.getPayload());
  }

  @Override
  public void sendPartialBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
    session.getRemote().sendPartialBytes(partialByte, isLast);
  }

  @Override
  public void sendPing(PingMessage message) throws IOException {
    session.getRemote().sendPing(message.getPayload());
  }

  @Override
  public void sendPong(PongMessage message) throws IOException {
    session.getRemote().sendPong(message.getPayload());
  }

  @Override
  public boolean isSecure() {
    return session.isSecure();
  }

  @Override
  public boolean isOpen() {
    return session.isOpen();
  }

  @Override
  public long getMaxIdleTimeout() {
    return session.getPolicy().getIdleTimeout();
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    session.getPolicy().setIdleTimeout(timeout);
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    session.getPolicy().setMaxBinaryMessageBufferSize(max);
  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return session.getPolicy().getMaxBinaryMessageBufferSize();
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    session.getPolicy().setMaxTextMessageBufferSize(max);
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return session.getPolicy().getMaxTextMessageBufferSize();
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    session.close(status.getCode(), status.getReason());
  }
}
