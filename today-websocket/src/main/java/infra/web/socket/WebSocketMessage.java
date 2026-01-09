/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import infra.core.io.buffer.DataBuffer;
import infra.lang.Assert;

/**
 * Representation of a WebSocket message.
 *
 * <p>See static factory methods in {@link WebSocketSession} for creating messages with
 * the {@link infra.core.io.buffer.DataBufferFactory} for the session.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/13 22:32
 */
public class WebSocketMessage {

  private final Type type;

  private final DataBuffer payload;

  @Nullable
  private final Object nativeMessage;

  private final boolean last;

  /**
   * Constructor for a WebSocketMessage.
   * <p>See static factory methods in {@link WebSocketSession} or alternatively
   * use {@link WebSocketSession#bufferFactory()} to create the payload and
   * then invoke this constructor.
   */
  public WebSocketMessage(Type type, DataBuffer payload, boolean last) {
    this(type, payload, null, last);
  }

  /**
   * Constructor for an inbound message with access to the underlying message.
   *
   * @param type the type of WebSocket message
   * @param payload the message content
   * @param nativeMessage the message from the API of the underlying WebSocket
   * library, if applicable.
   */
  public WebSocketMessage(Type type, DataBuffer payload, @Nullable Object nativeMessage, boolean last) {
    Assert.notNull(type, "'type' is required");
    Assert.notNull(payload, "'payload' is required");
    this.type = type;
    this.last = last;
    this.payload = payload;
    this.nativeMessage = nativeMessage;
  }

  /**
   * Return the message type (text, binary, etc).
   */
  public Type getType() {
    return this.type;
  }

  /**
   * Return the message payload.
   */
  public DataBuffer getPayload() {
    return this.payload;
  }

  /**
   * @return Whether the partial message being sent is the last part of the message.
   */
  public boolean isLast() {
    return last;
  }

  /**
   * Return the message from the API of the underlying WebSocket library. This
   * is applicable for inbound messages only and when the underlying message
   * has additional fields other than the content. Currently this is the case
   * for Reactor Netty only.
   *
   * @param <T> the type to cast the underlying message to
   * @return the underlying message, or {@code null}
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getNativeMessage() {
    return (T) this.nativeMessage;
  }

  /**
   * A variant of {@link #getPayloadAsText(Charset)} that uses {@code UTF-8}
   * for decoding the raw content to text.
   */
  public String getPayloadAsText() {
    return getPayloadAsText(StandardCharsets.UTF_8);
  }

  /**
   * A shortcut for decoding the raw content of the message to text with the
   * given character encoding. This is useful for text WebSocket messages, or
   * otherwise when the payload is expected to contain text.
   *
   * @param charset the character encoding
   */
  public String getPayloadAsText(Charset charset) {
    return this.payload.toString(charset);
  }

  /**
   * Retain the data buffer for the message payload, which is useful on
   * runtimes (for example, Netty) with pooled buffers. A shortcut for:
   * <pre>{@code
   * DataBuffer payload = message.getPayload();
   * payload.retain();
   * }</pre>
   *
   * @see DataBuffer#retain()
   */
  public WebSocketMessage retain() {
    payload.retain();
    return this;
  }

  /**
   * Retain the data buffer for the message payload, which is useful on
   * runtimes (for example, Netty) with pooled buffers. A shortcut for:
   * <pre>{@code
   * DataBuffer payload = message.getPayload();
   * payload.retainedDuplicate();
   * }</pre>
   *
   * @return Returns a retained message which shares the whole region of
   * payload buffer. Modifying the content of the returned buffer or this
   * buffer affects each other's content while they maintain separate
   * indexes and marks.
   * @see DataBuffer#retainedDuplicate()
   */
  public WebSocketMessage retainedDuplicate() {
    return new WebSocketMessage(type, payload.retainedDuplicate(), null, last);
  }

  /**
   * Release the payload {@code DataBuffer} which is useful on runtimes
   * (for example, Netty) with pooled buffers such as Netty. A shortcut for:
   * <pre>{@code
   * DataBuffer payload = message.getPayload();
   * payload.release();
   * }</pre>
   *
   * @see DataBuffer#release()
   */
  public void release() {
    payload.release();
  }

  /**
   * Creates websocket PING message
   */
  public static WebSocketMessage ping() {
    return ping(DataBuffer.empty());
  }

  /**
   * Creates websocket PING message
   *
   * @see WebSocketSession#bufferFactory()
   */
  public static WebSocketMessage ping(DataBuffer payload) {
    return new WebSocketMessage(Type.PING, payload, true);
  }

  /**
   * Creates websocket PONG message
   */
  public static WebSocketMessage pong() {
    return pong(DataBuffer.empty());
  }

  /**
   * Creates websocket PONG message
   *
   * @see WebSocketSession#bufferFactory()
   */
  public static WebSocketMessage pong(DataBuffer payload) {
    return new WebSocketMessage(Type.PONG, payload, true);
  }

  /**
   * Creates websocket BINARY message
   *
   * @see WebSocketSession#bufferFactory()
   */
  public static WebSocketMessage binary(DataBuffer payload) {
    return new WebSocketMessage(Type.BINARY, payload, true);
  }

  /**
   * Creates websocket BINARY message
   *
   * @see WebSocketSession#bufferFactory()
   */
  public static WebSocketMessage binary(DataBuffer payload, boolean last) {
    return new WebSocketMessage(Type.BINARY, payload, last);
  }

  /**
   * Creates websocket TEXT message
   *
   * @see WebSocketSession#bufferFactory()
   */
  public static WebSocketMessage text(DataBuffer payload) {
    return new WebSocketMessage(Type.TEXT, payload, true);
  }

  /**
   * Creates websocket TEXT message
   *
   * @see WebSocketSession#bufferFactory()
   */
  public static WebSocketMessage text(DataBuffer payload, boolean last) {
    return new WebSocketMessage(Type.TEXT, payload, last);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other || (other instanceof WebSocketMessage that && this.type == that.type
            && Objects.equals(this.payload, that.payload));
  }

  @Override
  public int hashCode() {
    return this.type.hashCode() * 29 + this.payload.hashCode();
  }

  @Override
  public String toString() {
    return "WebSocket %s message (%d bytes)".formatted(type.name(), payload.readableBytes());
  }

  /**
   * WebSocket message types.
   */
  public enum Type {
    /**
     * Text WebSocket message.
     */
    TEXT,

    /**
     * Binary WebSocket message.
     */
    BINARY,

    /**
     * WebSocket ping.
     */
    PING,

    /**
     * WebSocket pong.
     */
    PONG
  }

}
