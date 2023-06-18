/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import java.time.Duration;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
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
@SuppressWarnings("serial")
public class JettyWebSocketSession extends NativeWebSocketSession<Session> {

  @Nullable
  private String acceptedProtocol;

  public JettyWebSocketSession(HttpHeaders handshakeHeaders) {
    super(handshakeHeaders);
  }

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

  @Nullable
  @Override
  public String getAcceptedProtocol() {
    return acceptedProtocol;
  }

  @Override
  public long getMaxIdleTimeout() {
    return obtainNativeSession().getPolicy().getIdleTimeout().toMillis();
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    obtainNativeSession().getPolicy().setIdleTimeout(Duration.ofMillis(timeout));
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    obtainNativeSession().getPolicy().setMaxBinaryMessageSize(max);
  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return (int) obtainNativeSession().getPolicy().getMaxBinaryMessageSize();
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    obtainNativeSession().getPolicy().setMaxTextMessageSize(max);
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return (int) obtainNativeSession().getPolicy().getMaxTextMessageSize();
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    obtainNativeSession().close(status.getCode(), status.getReason());
  }

  @Override
  public void initializeNativeSession(Session session) {
    super.initializeNativeSession(session);
    this.acceptedProtocol = session.getUpgradeResponse().getAcceptedSubProtocol();
  }

}
