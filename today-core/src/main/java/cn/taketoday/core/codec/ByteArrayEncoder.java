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

package cn.taketoday.core.codec;

import org.reactivestreams.Publisher;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Encoder for {@code byte} arrays.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ByteArrayEncoder extends AbstractEncoder<byte[]> {

  public ByteArrayEncoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    Class<?> clazz = elementType.toClass();
    return super.canEncode(elementType, mimeType) && byte[].class.isAssignableFrom(clazz);
  }

  @Override
  public Flux<DataBuffer> encode(
          Publisher<? extends byte[]> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    // Use (byte[] bytes) for Eclipse
    return Flux.from(inputStream)
            .map((byte[] bytes) -> encodeValue(bytes, bufferFactory, elementType, mimeType, hints));
  }

  @Override
  public DataBuffer encodeValue(byte[] bytes, DataBufferFactory bufferFactory,
          ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    DataBuffer dataBuffer = bufferFactory.wrap(bytes);
    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      logger.debug("{}Writing {} bytes", Hints.getLogPrefix(hints), dataBuffer.readableByteCount());
    }
    return dataBuffer;
  }

}
