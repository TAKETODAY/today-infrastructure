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

package cn.taketoday.test.web.reactive.server;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;

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
    MimeType mimeType = MimeTypeUtils.APPLICATION_JSON;
    Map<String, Object> hints = Collections.emptyMap();

    DataBuffer buffer = ((Encoder<T>) this.encoder).encodeValue(
            (T) source, bufferFactory, ResolvableType.forInstance(source), mimeType, hints);

    return ((Decoder<T>) this.decoder).decode(buffer, targetType, mimeType, hints);
  }

}
