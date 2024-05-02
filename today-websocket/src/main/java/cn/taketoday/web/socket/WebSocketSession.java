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

package cn.taketoday.web.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.AlternativeJdkIdGenerator;

/**
 * A WebSocket session abstraction. Allows sending messages over a WebSocket
 * connection and closing it.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/5 14:16
 */
public abstract class WebSocketSession extends AttributeAccessorSupport implements AttributeAccessor {

  public static final AlternativeJdkIdGenerator idGenerator = new AlternativeJdkIdGenerator();

  private final String id;

  private final HttpHeaders handshakeHeaders;

  public WebSocketSession(@Nullable HttpHeaders handshakeHeaders) {
    this.handshakeHeaders = handshakeHeaders;
    this.id = handshakeHeaders == null ? null : idGenerator.generateId().toString();
  }

  /**
   * Session Id
   */
  public String getId() {
    return id;
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
  public void sendMessage(Message<?> message) throws IOException {
    if (message instanceof TextMessage) {
      sendText((String) message.getPayload());
    }
    else if (message instanceof BinaryMessage) {
      sendBinary((BinaryMessage) message);
    }
    else if (message instanceof PingMessage) {
      sendPing((PingMessage) message);
    }
    else if (message instanceof PongMessage) {
      sendPong((PongMessage) message);
    }
    else {
      throw new IllegalStateException("Unexpected WebSocketMessage type: " + message);
    }
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
  public void sendPartialMessage(Message<?> message) throws IOException {
    if (message instanceof TextMessage) {
      sendPartialText((TextMessage) message);
    }
    else if (message instanceof BinaryMessage) {
      sendPartialBinary((BinaryMessage) message);
    }
    else if (message instanceof PingMessage) {
      sendPing((PingMessage) message);
    }
    else if (message instanceof PongMessage) {
      sendPong((PongMessage) message);
    }
    else {
      throw new IllegalStateException("Unexpected WebSocketMessage type: " + message);
    }
  }

  /**
   * Send a text message, blocking until all of the message has been transmitted.
   *
   * @param text the message to be sent.
   * @throws IOException if there is a problem delivering the message.
   */
  public abstract void sendText(String text) throws IOException;

  /**
   * Send a text message in parts, blocking until all of the message has been transmitted. The runtime
   * reads the message in order. Non-final parts of the message are sent with isLast set to false. The final part
   * must be sent with isLast set to true.
   *
   * @param partialMessage the parts of the message being sent.
   * @throws IOException if there is a problem delivering the message fragment.
   */
  public void sendPartialText(TextMessage partialMessage) throws IOException {
    sendPartialText(partialMessage.getPayload(), partialMessage.isLast());
  }

  /**
   * Send a text message in parts, blocking until all of the message has been transmitted. The runtime
   * reads the message in order. Non-final parts of the message are sent with isLast set to false. The final part
   * must be sent with isLast set to true.
   *
   * @param partialMessage the parts of the message being sent.
   * @param isLast Whether the partial message being sent is the last part of the message.
   * @throws IOException if there is a problem delivering the message fragment.
   */
  public abstract void sendPartialText(String partialMessage, boolean isLast)
          throws IOException;

  /**
   * Send a binary message, returning when all of the message has been transmitted.
   *
   * @param data the message to be sent.
   * @throws IOException if there is a problem delivering the message.
   */
  public abstract void sendBinary(BinaryMessage data) throws IOException;

  public void sendPartialBinary(BinaryMessage data) throws IOException {
    sendPartialBinary(data.getPayload(), data.isLast());
  }

  /**
   * Send a binary message in parts, blocking until all of the message has been transmitted. The runtime
   * reads the message in order. Non-final parts are sent with isLast set to false. The final piece
   * must be sent with isLast set to true.
   *
   * @param partialByte the part of the message being sent.
   * @param isLast Whether the partial message being sent is the last part of the message.
   * @throws IOException if there is a problem delivering the partial message.
   */
  public abstract void sendPartialBinary(ByteBuffer partialByte, boolean isLast)
          throws IOException;

  public abstract void sendPing(PingMessage message) throws IOException;

  public abstract void sendPong(PongMessage message) throws IOException;

  public abstract boolean isSecure();

  public abstract boolean isOpen();

  /**
   * Get the idle timeout for this session.
   *
   * @return The current idle timeout for this session in milliseconds. Zero
   * or negative values indicate an infinite timeout.
   */
  public abstract long getMaxIdleTimeout();

  /**
   * Set the idle timeout for this session.
   *
   * @param timeout The new idle timeout for this session in milliseconds.
   * Zero or negative values indicate an infinite timeout.
   */
  public abstract void setMaxIdleTimeout(long timeout);

  /**
   * Set the current maximum buffer size for binary messages.
   *
   * @param max The new maximum buffer size in bytes
   */
  public abstract void setMaxBinaryMessageBufferSize(int max);

  /**
   * Get the current maximum buffer size for binary messages.
   *
   * @return The current maximum buffer size in bytes
   */
  public abstract int getMaxBinaryMessageBufferSize();

  /**
   * Set the maximum buffer size for text messages.
   *
   * @param max The new maximum buffer size in characters.
   */
  public abstract void setMaxTextMessageBufferSize(int max);

  /**
   * Get the maximum buffer size for text messages.
   *
   * @return The maximum buffer size in characters.
   */
  public abstract int getMaxTextMessageBufferSize();

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
   * Return the headers used in the handshake request (never {@code null}).
   *
   * @since 4.0
   */
  public HttpHeaders getHandshakeHeaders() {
    return handshakeHeaders;
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
