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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import infra.core.AttributeAccessor;
import infra.core.AttributeAccessorSupport;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.lang.Nullable;
import infra.util.AlternativeJdkIdGenerator;
import infra.util.concurrent.Future;

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
   * Return a {@code DataBuffer} Factory to create message payloads.
   *
   * @return the buffer factory for the session
   * @since 5.0
   */
  public abstract DataBufferFactory bufferFactory();

  /**
   * write the messages and return a {@code Future<Void>} that
   * completes when the source completes and writing is done.
   *
   * @since 5.0
   */
  public abstract Future<Void> send(WebSocketMessage message);

  /**
   * write the messages and return a {@code Future<Void>} that
   * completes when the source completes and writing is done.
   *
   * @since 5.0
   */
  public Future<Void> send(Future<WebSocketMessage> message) {
    return message.flatMap(this::send);
  }

  /**
   * Send a text message, blocking until all of the message has been transmitted.
   *
   * @param text the message to be sent.
   */
  public Future<Void> sendText(CharSequence text) {
    return send(textMessage(text));
  }

  /**
   * Send a binary message, returning when all of the message has been transmitted.
   *
   * @param payload the message to be sent.
   * @since 5.0
   */
  public Future<Void> sendBinary(DataBuffer payload) {
    return send(WebSocketMessage.binary(payload));
  }

  /**
   * Send a binary message, returning when all of the message has been transmitted.
   *
   * @param payloadFactory the message factory to be sent.
   * @since 5.0
   */
  public Future<Void> sendBinary(Function<DataBufferFactory, DataBuffer> payloadFactory) {
    return sendBinary(payloadFactory.apply(bufferFactory()));
  }

  /**
   * Send a binary message, returning when all of the message has been transmitted.
   *
   * @param payloadFactory the message factory to be sent.
   * @since 5.0
   */
  public Future<Void> send(Function<DataBufferFactory, WebSocketMessage> payloadFactory) {
    return send(payloadFactory.apply(bufferFactory()));
  }

  /**
   * Send ping message
   *
   * @since 5.0
   */
  public Future<Void> sendPing() {
    return send(WebSocketMessage.ping());
  }

  /**
   * Send pong message
   *
   * @since 5.0
   */
  public Future<Void> sendPong() {
    return send(WebSocketMessage.pong());
  }

  /**
   * Factory method to create a text {@link WebSocketMessage} using the
   * {@link #bufferFactory()} for the session.
   *
   * @since 5.0
   */
  public WebSocketMessage textMessage(CharSequence payload) {
    DataBuffer buffer = bufferFactory().copiedBuffer(payload, StandardCharsets.UTF_8);
    return WebSocketMessage.text(buffer, true);
  }

  /**
   * Factory method to create a binary WebSocketMessage using the
   * {@link #bufferFactory()} for the session.
   *
   * @since 5.0
   */
  public WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
    DataBuffer payload = payloadFactory.apply(bufferFactory());
    return WebSocketMessage.binary(payload);
  }

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
   */
  public Future<Void> close() {
    return close(CloseStatus.NORMAL);
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
   */
  public abstract Future<Void> close(CloseStatus status);

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
