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

package infra.test.web.reactive.server;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Decoder;
import infra.core.codec.Encoder;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.util.MimeType;

/**
 * JSON Path {@link MappingProvider} implementation using {@link Encoder}
 * and {@link Decoder}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class EncoderDecoderMappingProvider implements MappingProvider {

  private final Encoder<?> encoder;

  private final Decoder<?> decoder;

  /**
   * Create an instance with the specified writers and readers.
   */
  public EncoderDecoderMappingProvider(Encoder<?> encoder, Decoder<?> decoder) {
    this.encoder = encoder;
    this.decoder = decoder;
  }

  @Nullable
  @Override
  public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
    return mapToTargetType(source, ResolvableType.forClass(targetType));
  }

  @Nullable
  @Override
  public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
    return mapToTargetType(source, ResolvableType.forType(targetType.getType()));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <T> T mapToTargetType(Object source, ResolvableType targetType) {
    DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;
    MimeType mimeType = MimeType.APPLICATION_JSON;
    Map<String, Object> hints = Collections.emptyMap();

    DataBuffer buffer = ((Encoder<T>) this.encoder).encodeValue(
            (T) source, bufferFactory, ResolvableType.forInstance(source), mimeType, hints);

    return ((Decoder<T>) this.decoder).decode(buffer, targetType, mimeType, hints);
  }

}
