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

package infra.messaging.rsocket.annotation.support;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import infra.core.MethodParameter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.codec.Encoder;
import infra.core.io.buffer.DataBuffer;
import infra.lang.Assert;
import infra.messaging.Message;
import infra.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import infra.messaging.rsocket.PayloadUtils;
import io.rsocket.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Extension of {@link AbstractEncoderMethodReturnValueHandler} that
 * {@link #handleEncodedContent handles} encoded content by wrapping data buffers
 * as RSocket payloads and by passing those through the {@link #RESPONSE_HEADER}
 * header.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RSocketPayloadReturnValueHandler extends AbstractEncoderMethodReturnValueHandler {

  /**
   * Message header name that is expected to have an {@link java.util.concurrent.atomic.AtomicReference}
   * which will receive the {@code Flux<Payload>} that represents the response.
   */
  public static final String RESPONSE_HEADER = "rsocketResponse";

  public RSocketPayloadReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
    super(encoders, registry);
  }

  @Override
  protected Mono<Void> handleEncodedContent(
          Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message) {

    AtomicReference<Flux<Payload>> responseRef = getResponseReference(message);
    Assert.notNull(responseRef, "Missing '" + RESPONSE_HEADER + "'");
    responseRef.set(encodedContent.map(PayloadUtils::createPayload));
    return Mono.empty();
  }

  @Override
  protected Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message) {
    AtomicReference<Flux<Payload>> responseRef = getResponseReference(message);
    if (responseRef != null) {
      responseRef.set(Flux.empty());
    }
    return Mono.empty();
  }

  @SuppressWarnings("unchecked")
  private @Nullable AtomicReference<Flux<Payload>> getResponseReference(Message<?> message) {
    Object headerValue = message.getHeaders().get(RESPONSE_HEADER);
    Assert.state(headerValue == null || headerValue instanceof AtomicReference, "Expected AtomicReference");
    return (AtomicReference<Flux<Payload>>) headerValue;
  }

}
