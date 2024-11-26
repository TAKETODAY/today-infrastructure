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
import infra.core.io.buffer.Netty5DataBufferFactory;
import infra.lang.Nullable;
import infra.util.MimeType;
import io.netty5.buffer.Buffer;
import reactor.core.publisher.Flux;

/**
 * Encoder for {@link Buffer Buffers}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Netty5BufferEncoder extends AbstractEncoder<Buffer> {

  public Netty5BufferEncoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canEncode(ResolvableType type, @Nullable MimeType mimeType) {
    Class<?> clazz = type.toClass();
    return super.canEncode(type, mimeType) && Buffer.class.isAssignableFrom(clazz);
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<? extends Buffer> inputStream,
          DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
          @Nullable Map<String, Object> hints) {

    return Flux.from(inputStream).map(byteBuffer ->
            encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
  }

  @Override
  public DataBuffer encodeValue(Buffer buffer, DataBufferFactory bufferFactory, ResolvableType valueType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      String logPrefix = Hints.getLogPrefix(hints);
      logger.debug(logPrefix + "Writing " + buffer.readableBytes() + " bytes");
    }
    if (bufferFactory instanceof Netty5DataBufferFactory netty5DataBufferFactory) {
      return netty5DataBufferFactory.wrap(buffer);
    }
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.readBytes(bytes, 0, bytes.length);
    buffer.close();
    return bufferFactory.wrap(bytes);
  }
}
