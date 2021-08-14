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
import cn.taketoday.web.socket.NativeWebSocketSession;
import cn.taketoday.web.socket.PingMessage;
import cn.taketoday.web.socket.PongMessage;

/**
 * Jetty WebSocketSession
 *
 * @author TODAY 2021/5/6 21:40
 * @since 3.0.1
 */
public class JettyWebSocketSession extends NativeWebSocketSession<Session> {

  @Override
  public void sendText(String text) throws IOException {
    obtainNativeSession().getRemote().sendString(text);
  }

  @Override
  public void sendPartialText(String partialMessage, boolean isLast) throws IOException {
    obtainNativeSession().getRemote().sendPartialString(partialMessage, isLast);
  }

  @Override
  public void sendBinary(BinaryMessage data) throws IOException {
    obtainNativeSession().getRemote().sendBytes(data.getPayload());
  }

  @Override
  public void sendPartialBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
    obtainNativeSession().getRemote().sendPartialBytes(partialByte, isLast);
  }

  @Override
  public void sendPing(PingMessage message) throws IOException {
    obtainNativeSession().getRemote().sendPing(message.getPayload());
  }

  @Override
  public void sendPong(PongMessage message) throws IOException {
    obtainNativeSession().getRemote().sendPong(message.getPayload());
  }

  @Override
  public boolean isSecure() {
    return obtainNativeSession().isSecure();
  }

  @Override
  public boolean isOpen() {
    return obtainNativeSession().isOpen();
  }

  @Override
  public long getMaxIdleTimeout() {
    return obtainNativeSession().getPolicy().getIdleTimeout();
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    obtainNativeSession().getPolicy().setIdleTimeout(timeout);
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    obtainNativeSession().getPolicy().setMaxBinaryMessageBufferSize(max);
  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return obtainNativeSession().getPolicy().getMaxBinaryMessageBufferSize();
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    obtainNativeSession().getPolicy().setMaxTextMessageBufferSize(max);
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return obtainNativeSession().getPolicy().getMaxTextMessageBufferSize();
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    obtainNativeSession().close(status.getCode(), status.getReason());
  }

}
