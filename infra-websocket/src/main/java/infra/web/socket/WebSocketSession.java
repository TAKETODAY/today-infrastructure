/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.socket;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import infra.core.AttributeAccessor;
import infra.core.DefaultAttributeAccessor;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.util.JdkUuidGenerator;
import infra.util.concurrent.Future;

/**
 * Abstract base class for a WebSocket connection, providing a session identifier,
 * attributes, connection metadata, and asynchronous send and close operations.
 *
 * <p>Send methods return a {@link Future} that completes successfully when the
 * underlying transport has completed the write, or fails if the operation fails.
 * Successful completion does not imply that the peer has received or processed
 * the message. Use the returned future to observe completion or compose
 * subsequent operations.
 *
 * <p>Message creation, encoding, and factory invocation take place on the calling
 * thread and may throw synchronously. Transport failures are reported through the
 * returned future. Callback execution follows the future's executor; use
 * {@link Future#publishOn(java.util.concurrent.Executor)} to select another
 * executor. Do not block an I/O event-loop thread while waiting for completion.
 *
 * <p>A send future provides completion notification, not automatic backpressure.
 * Applications should bound outstanding sends, for example by composing successive
 * sends with {@code flatMap} rather than submitting an unbounded number of writes.
 * Cancelling an operation's future does not guarantee that the transport operation
 * is cancelled or that an already submitted message is withdrawn.
 *
 * <p>Message factory methods use the session's {@link #bufferFactory()} to create
 * payloads compatible with the underlying transport. They create messages without
 * sending them. Sending order, concurrent-send support, and payload ownership
 * depend on the concrete implementation.
 *
 * <p>Use {@link #close(CloseStatus)} to initiate closure with a WebSocket close
 * frame, or {@link #abort()} to terminate the underlying connection abruptly.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/5 14:16
 */
public abstract class WebSocketSession extends DefaultAttributeAccessor implements AttributeAccessor {

  protected static final JdkUuidGenerator idGenerator = new JdkUuidGenerator();

  private final String id = idGenerator.generateId().toString();

  /**
   * Return the unique identifier generated for this session.
   *
   * @return the session identifier, stable for the lifetime of this instance
   */
  public String getId() {
    return id;
  }

  /**
   * Return the buffer factory for creating message payloads compatible with
   * this session's underlying transport.
   *
   * @return the buffer factory for the session
   * @since 5.0
   */
  public abstract DataBufferFactory bufferFactory();

  /**
   * Send the given WebSocket message asynchronously.
   *
   * @param message the message to send
   * @return a future that completes when the write completes, or fails if
   * the write fails
   * @since 5.0
   */
  public abstract Future<Void> send(WebSocketMessage message);

  /**
   * Send the message supplied by the given future once it completes successfully.
   *
   * <p>If the supplied future fails, no message is sent and the returned future
   * propagates that failure.
   *
   * @param message a future supplying the message to send
   * @return a future that completes when the message write completes, or fails
   * if obtaining or sending the message fails
   * @since 5.0
   */
  public Future<Void> send(Future<WebSocketMessage> message) {
    return message.flatMap(this::send);
  }

  /**
   * Send the given text asynchronously, encoded as UTF-8.
   *
   * @param text the text to send
   * @return a future that completes when the write completes, or fails if
   * the write fails
   */
  public Future<Void> sendText(CharSequence text) {
    return send(textMessage(text));
  }

  /**
   * Send the given payload asynchronously as a binary message.
   *
   * @param payload the binary payload to send
   * @return a future that completes when the write completes, or fails if
   * the write fails
   * @since 5.0
   */
  public Future<Void> sendBinary(DataBuffer payload) {
    return send(WebSocketMessage.binary(payload));
  }

  /**
   * Create a binary payload with the session's {@link #bufferFactory()} and
   * send it asynchronously.
   *
   * <p>The factory is invoked synchronously when this method is called.
   *
   * @param payloadFactory a function that creates the binary payload
   * @return a future that completes when the write completes, or fails if
   * the write fails
   * @since 5.0
   */
  public Future<Void> sendBinary(Function<DataBufferFactory, DataBuffer> payloadFactory) {
    return sendBinary(payloadFactory.apply(bufferFactory()));
  }

  /**
   * Create a WebSocket message with the session's {@link #bufferFactory()} and
   * send it asynchronously.
   *
   * <p>The factory is invoked synchronously when this method is called. The
   * returned message determines the message type.
   *
   * @param payloadFactory a function that creates the message to send
   * @return a future that completes when the write completes, or fails if
   * the write fails
   * @since 5.0
   */
  public Future<Void> send(Function<DataBufferFactory, WebSocketMessage> payloadFactory) {
    return send(payloadFactory.apply(bufferFactory()));
  }

  /**
   * Send a ping control frame with an empty payload asynchronously.
   *
   * @return a future that completes when the write completes, or fails if
   * the write fails; completion does not indicate receipt of a pong response
   * @since 5.0
   */
  public Future<Void> sendPing() {
    return send(WebSocketMessage.ping());
  }

  /**
   * Send a pong control frame with an empty payload asynchronously.
   *
   * @return a future that completes when the write completes, or fails if
   * the write fails
   * @since 5.0
   */
  public Future<Void> sendPong() {
    return send(WebSocketMessage.pong());
  }

  /**
   * Create a text message by copying the given text into a UTF-8 encoded buffer
   * from the session's {@link #bufferFactory()}.
   *
   * @param payload the text content
   * @return the text message, without sending it
   * @since 5.0
   */
  public WebSocketMessage textMessage(CharSequence payload) {
    DataBuffer buffer = bufferFactory().copiedBuffer(payload, StandardCharsets.UTF_8);
    return WebSocketMessage.text(buffer, true);
  }

  /**
   * Create a binary message using the session's {@link #bufferFactory()}.
   *
   * @param payloadFactory a function that creates the binary payload, invoked
   * synchronously by this method
   * @return the binary message, without sending it
   * @since 5.0
   */
  public WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
    DataBuffer payload = payloadFactory.apply(bufferFactory());
    return WebSocketMessage.binary(payload);
  }

  /**
   * Return whether this session uses a secure WebSocket connection (WSS).
   *
   * @return {@code true} if the underlying transport is secured with TLS
   */
  public abstract boolean isSecure();

  /**
   * Return whether the underlying connection is open.
   *
   * <p>An open connection is not necessarily active or ready to send messages.
   *
   * @return {@code true} if the connection has not been closed
   * @see #isActive()
   */
  public abstract boolean isOpen();

  /**
   * Return whether the underlying connection is active and connected.
   *
   * <p>The default implementation delegates to {@link #isOpen()}. Subclasses
   * may distinguish an open connection from an active connection.
   *
   * @return {@code true} if the connection is active
   * @since 5.0
   */
  public boolean isActive() {
    return isOpen();
  }

  /**
   * Initiate abrupt termination of the underlying connection without sending
   * a WebSocket close frame.
   *
   * <p>Termination may complete asynchronously after this method returns.
   * Pending send operations may fail as a result.
   *
   * @return a future that completes when the underlying connection is closed,
   * or fails if termination fails
   * @see #close(CloseStatus)
   */
  public abstract Future<Void> abort();

  /**
   * Initiate closure with {@link CloseStatus#NORMAL} and no reason phrase.
   *
   * @return a future representing the close operation
   * @see #close(CloseStatus)
   */
  public Future<Void> close() {
    return close(CloseStatus.NORMAL);
  }

  /**
   * Initiate closure by sending a WebSocket close frame with the given status
   * code and optional reason.
   *
   * <p>The status code and reason must be valid for transmission according to
   * the WebSocket protocol. Reserved codes such as
   * {@link CloseStatus#NO_STATUS_CODE} must not be sent in a close frame.
   *
   * <p>The returned future completes successfully after the close frame has been
   * written and the underlying local connection has been closed. This does not
   * indicate that the peer has replied with a close frame. If writing the close
   * frame fails, the implementation still attempts to close the connection and
   * reports the write failure.
   *
   * <p>Implementations should make closure idempotent and reject new sends once
   * closure begins. Timeout and concurrent-abort policies are implementation-specific.
   *
   * @param status the close status code and optional reason to send to the peer
   * @return a future that completes when the close operation completes, or
   * fails if the operation fails
   * @see #abort()
   */
  public abstract Future<Void> close(CloseStatus status);

  /**
   * Return the local address of the underlying connection, if available.
   *
   * @return the local address, or {@code null} if unavailable
   * @since 4.0
   */
  public @Nullable InetSocketAddress getLocalAddress() {
    return null;
  }

  /**
   * Return the address of the remote peer, if available.
   *
   * @return the remote address, or {@code null} if unavailable
   * @since 4.0
   */
  public @Nullable InetSocketAddress getRemoteAddress() {
    return null;
  }

  /**
   * Return the subprotocol negotiated during the WebSocket handshake.
   *
   * @return the protocol identifier, or {@code null} if no protocol
   * was negotiated
   * @since 4.0
   */
  public abstract @Nullable String getAcceptedProtocol();

}
