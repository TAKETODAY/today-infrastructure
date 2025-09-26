/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.util.MimeType;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

/**
 * Encoder for {@link ByteBuf ByteBufs}.
 *
 * @author Vladislav Kisel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class NettyByteBufEncoder extends AbstractEncoder<ByteBuf> {

  public NettyByteBufEncoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canEncode(ResolvableType type, @Nullable MimeType mimeType) {
    Class<?> clazz = type.toClass();
    return super.canEncode(type, mimeType) && ByteBuf.class.isAssignableFrom(clazz);
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<? extends ByteBuf> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(inputStream)
            .map(byteBuffer -> encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
  }

  @Override
  public DataBuffer encodeValue(ByteBuf byteBuf, DataBufferFactory bufferFactory,
          ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      logger.debug("{}Writing {} bytes", Hints.getLogPrefix(hints), byteBuf.readableBytes());
    }
    if (bufferFactory instanceof NettyDataBufferFactory) {
      return ((NettyDataBufferFactory) bufferFactory).wrap(byteBuf);
    }
    byte[] bytes = new byte[byteBuf.readableBytes()];
    byteBuf.readBytes(bytes);
    byteBuf.release();
    return bufferFactory.wrap(bytes);
  }
}
