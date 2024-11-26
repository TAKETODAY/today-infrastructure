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

package infra.core.codec;

import org.reactivestreams.Publisher;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.lang.Nullable;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Simple pass-through encoder for {@link DataBuffer DataBuffers}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class DataBufferEncoder extends AbstractEncoder<DataBuffer> {

  public DataBufferEncoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    Class<?> clazz = elementType.toClass();
    return super.canEncode(elementType, mimeType) && DataBuffer.class.isAssignableFrom(clazz);
  }

  @Override
  public Flux<DataBuffer> encode(
          Publisher<? extends DataBuffer> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<DataBuffer> flux = Flux.from(inputStream);
    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      flux = flux.doOnNext(buffer -> logValue(buffer, hints));
    }
    return flux;
  }

  @Override
  public DataBuffer encodeValue(
          DataBuffer buffer, DataBufferFactory bufferFactory,
          ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      logValue(buffer, hints);
    }
    return buffer;
  }

  private void logValue(DataBuffer buffer, @Nullable Map<String, Object> hints) {
    logger.debug("{} Writing {} bytes", Hints.getLogPrefix(hints), buffer.readableByteCount());
  }

}
