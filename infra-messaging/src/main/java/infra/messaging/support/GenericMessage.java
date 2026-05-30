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

package infra.messaging.support;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Map;

import infra.lang.Assert;
import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.util.ObjectUtils;

/**
 * An implementation of {@link Message} with a generic payload.
 * Once created, a GenericMessage is immutable.
 *
 * @param <T> the payload type
 * @author Mark Fisher
 * @see MessageBuilder
 * @since 5.0
 */
public class GenericMessage<T> implements Message<T>, Serializable {

  private static final long serialVersionUID = 4268801052358035098L;

  @SuppressWarnings("serial")
  private final T payload;

  private final MessageHeaders headers;

  /**
   * Create a new message with the given payload.
   *
   * @param payload the message payload (never {@code null})
   */
  public GenericMessage(T payload) {
    this(payload, new MessageHeaders(null));
  }

  /**
   * Create a new message with the given payload and headers.
   * The content of the given header map is copied.
   *
   * @param payload the message payload (never {@code null})
   * @param headers message headers to use for initialization
   */
  public GenericMessage(T payload, Map<String, Object> headers) {
    this(payload, new MessageHeaders(headers));
  }

  /**
   * A constructor with the {@link MessageHeaders} instance to use.
   * <p><strong>Note:</strong> the given {@code MessageHeaders} instance is used
   * directly in the new message, i.e. it is not copied.
   *
   * @param payload the message payload (never {@code null})
   * @param headers message headers
   */
  public GenericMessage(T payload, MessageHeaders headers) {
    Assert.notNull(payload, "Payload is required");
    Assert.notNull(headers, "MessageHeaders is required");
    this.payload = payload;
    this.headers = headers;
  }

  @Override
  public T getPayload() {
    return this.payload;
  }

  @Override
  public MessageHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    // Using nullSafeEquals for proper array equals comparisons
    return (this == other || (other instanceof GenericMessage<?> that &&
            ObjectUtils.nullSafeEquals(this.payload, that.payload) && this.headers.equals(that.headers)));
  }

  @Override
  public int hashCode() {
    // Using nullSafeHashCode for proper array hashCode handling
    return ObjectUtils.nullSafeHash(this.payload, this.headers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append(" [payload=");
    if (this.payload instanceof byte[] bytes) {
      sb.append("byte[").append(bytes.length).append(']');
    }
    else {
      sb.append(this.payload);
    }
    sb.append(", headers=").append(this.headers).append(']');
    return sb.toString();
  }

}
