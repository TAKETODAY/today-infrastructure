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

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.core.io.buffer.Netty5DataBuffer;
import infra.lang.Nullable;
import infra.util.MimeType;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.DefaultBufferAllocators;

/**
 * Decoder for {@link Buffer Buffers}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Netty5BufferDecoder extends AbstractDataBufferDecoder<Buffer> {

  public Netty5BufferDecoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return (Buffer.class.isAssignableFrom(elementType.toClass()) &&
            super.canDecode(elementType, mimeType));
  }

  @Override
  public Buffer decode(DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (logger.isDebugEnabled()) {
      logger.debug("%sRead %d bytes".formatted(Hints.getLogPrefix(hints), dataBuffer.readableBytes()));
    }
    if (dataBuffer instanceof Netty5DataBuffer netty5DataBuffer) {
      return netty5DataBuffer.getNativeBuffer();
    }
    byte[] bytes = new byte[dataBuffer.readableBytes()];
    dataBuffer.read(bytes);
    Buffer buffer = DefaultBufferAllocators.preferredAllocator().copyOf(bytes);
    DataBufferUtils.release(dataBuffer);
    return buffer;
  }

}
