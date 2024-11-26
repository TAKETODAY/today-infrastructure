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
import infra.lang.Nullable;
import infra.util.MimeType;

/**
 * Decoder for {@code byte} arrays.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ByteArrayDecoder extends AbstractDataBufferDecoder<byte[]> {

  public ByteArrayDecoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return elementType.resolve() == byte[].class && super.canDecode(elementType, mimeType);
  }

  @Override
  public byte[] decode(DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    byte[] result = new byte[dataBuffer.readableByteCount()];
    dataBuffer.read(result);
    DataBufferUtils.release(dataBuffer);
    if (logger.isDebugEnabled()) {
      logger.debug("{}Read {} bytes", Hints.getLogPrefix(hints), result.length);
    }
    return result;
  }

}
