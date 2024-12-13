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

package infra.web.socket;

import java.io.IOException;
import java.net.InetSocketAddress;

import infra.core.AttributeAccessor;
import infra.core.AttributeAccessorSupport;
import infra.core.io.buffer.DataBuffer;
import infra.lang.Nullable;
import infra.util.AlternativeJdkIdGenerator;

/**
 * A WebSocket session abstraction. Allows sending messages over a WebSocket
 * connection and closing it.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/5 14:16
 */
public abstract class WebSocketSession extends AttributeAccessorSupport implements AttributeAccessor {

  public static final AlternativeJdkIdGenerator idGenerator = new AlternativeJdkIdGenerator();

  private final String id = idGenerator.generateId().toString();

  /**
   * Session Id
   */
  public String getId() {
    return id;
  }

  /**
   * Send a text message, blocking until all of the message has been transmitted.
   *
   * @param text the message to be sent.
   * @throws IOException if there is a problem delivering the message.
   */
  public void sendText(CharSequence text) throws IOException {
    sendMessage(new TextMessage(text, true));
  }

  /**
   * Send a binary message, returning when all of the message has been transmitted.
   *
   * @param buffer the message to be sent.
   * @throws IOException if there is a problem delivering the message.
   * @since 5.0
   */
  public void sendBinary(DataBuffer buffer) throws IOException {
    sendMessage(new BinaryMessage(buffer));
  }

  /**
   * Send ping message
   *
   * @since 5.0
   */
  public void sendPing() throws IOException {
    sendMessage(new PingMessage());
  }

  /**
   * Send pong message
   *
   * @since 5.0
   */
  public void sendPong() throws IOException {
    sendMessage(new PongMessage());
  }

  /**
   * Send a message in parts, blocking until all of the message has
   * been transmitted. The runtime reads the message in order.
   * Non-final parts of the message are sent with isLast set to false.
   * The final part must be sent with isLast set to true.
   *
   * @param message Message
   * @throws IOException if there is a problem delivering the message.
   * @see Message#isLast()
   */
  public abstract void sendMessage(Message<?> message) throws IOException;

  /**
   * is WSS ?
   */
  public abstract boolean isSecure();

  /**
   * Returns {@code true} if the channel is open and may get active later
   */
  public abstract boolean isOpen();

  /**
   * return {@code true} if the channel is active and so connected.
   *
   * @since 5.0
   */
  public boolean isActive() {
    return isOpen();
  }

  /**
   * Close the current conversation with a normal status code and no reason phrase.
   *
   * @throws IOException if there was a connection error closing the connection.
   */
  public void close() throws IOException {
    close(CloseStatus.NORMAL);
  }

  /**
   * Close the current conversation, giving a reason for the closure. The close
   * call causes the implementation to attempt notify the client of the close as
   * soon as it can. This may cause the sending of unsent messages immediately
   * prior to the close notification. After the close notification has been sent
   * the implementation notifies the endpoint's onClose method. Note the websocket
   * specification defines the
   * acceptable uses of status codes and reason phrases. If the application cannot
   * determine a suitable close code to use for the closeReason, it is recommended
   * to use {@link CloseStatus#NO_STATUS_CODE}.
   *
   * @param status the reason for the closure.
   * @throws IOException if there was a connection error closing the connection
   */
  public abstract void close(CloseStatus status) throws IOException;

  /**
   * Return the address on which the request was received.
   *
   * @since 4.0
   */
  @Nullable
  public InetSocketAddress getLocalAddress() {
    return null;
  }

  /**
   * Return the address of the remote client.
   *
   * @since 4.0
   */
  @Nullable
  public InetSocketAddress getRemoteAddress() {
    return null;
  }

  /**
   * Return the negotiated sub-protocol.
   *
   * @return the protocol identifier, or {@code null} if no protocol
   * was specified or negotiated successfully
   * @since 4.0
   */
  @Nullable
  public abstract String getAcceptedProtocol();

}
