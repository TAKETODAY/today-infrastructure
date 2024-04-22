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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.jetty;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0.1 2021/5/6 21:40
 */
public class JettyWebSocketSession extends NativeWebSocketSession<Session> {

  @Nullable
  private String acceptedProtocol;

  public JettyWebSocketSession(HttpHeaders handshakeHeaders) {
    super(handshakeHeaders);
  }

  @Override
  public void sendText(String text) throws IOException {
    useSession((session, callback) -> session.sendText(text, callback));
  }

  @Override
  public void sendPartialText(String partialMessage, boolean isLast) throws IOException {
    useSession((session, callback) -> session.sendPartialText(partialMessage, isLast, callback));
  }

  @Override
  public void sendBinary(BinaryMessage data) throws IOException {
    useSession((session, callback) -> session.sendBinary(data.getPayload(), callback));
  }

  @Override
  public void sendPartialBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
    useSession((session, callback) -> session.sendPartialBinary(partialByte, isLast, callback));
  }

  @Override
  public void sendPing(PingMessage message) throws IOException {
    useSession((session, callback) -> session.sendPing(message.getPayload(), callback));
  }

  @Override
  public void sendPong(PongMessage message) throws IOException {
    useSession((session, callback) -> session.sendPong(message.getPayload(), callback));
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
    checkNativeSessionInitialized();
    return acceptedProtocol;
  }

  @Override
  public long getMaxIdleTimeout() {
    return obtainNativeSession().getIdleTimeout().toMillis();
  }

  @Override
  public void setMaxIdleTimeout(long timeout) {
    obtainNativeSession().setIdleTimeout(Duration.ofMillis(timeout));
  }

  @Override
  public void setMaxBinaryMessageBufferSize(int max) {
    obtainNativeSession().setMaxBinaryMessageSize(max);
  }

  @Override
  public int getMaxBinaryMessageBufferSize() {
    return (int) obtainNativeSession().getMaxBinaryMessageSize();
  }

  @Override
  public void setMaxTextMessageBufferSize(int max) {
    obtainNativeSession().setMaxTextMessageSize(max);
  }

  @Override
  public int getMaxTextMessageBufferSize() {
    return (int) obtainNativeSession().getMaxTextMessageSize();
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    useSession((session, callback) -> session.close(status.getCode(), status.getReason(), callback));
  }

  @Override
  public void initializeNativeSession(Session session) {
    super.initializeNativeSession(session);
    this.acceptedProtocol = session.getUpgradeResponse().getAcceptedSubProtocol();
  }

  private void useSession(SessionConsumer sessionConsumer) throws IOException {
    try {
      Callback.Completable completable = new Callback.Completable();
      sessionConsumer.consume(obtainNativeSession(), completable);
      completable.get();
    }
    catch (ExecutionException ex) {
      Throwable cause = ex.getCause();

      if (cause instanceof IOException ioEx) {
        throw ioEx;
      }
      else if (cause instanceof UncheckedIOException uioEx) {
        throw uioEx.getCause();
      }
      else {
        throw new IOException(ex.getMessage(), cause);
      }
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private interface SessionConsumer {
    void consume(Session session, Callback callback) throws IOException;
  }

}
