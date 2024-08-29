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

import java.nio.ByteBuffer;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;

/**
 * Decoder for {@link ByteBuffer ByteBuffers}.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ByteBufferDecoder extends AbstractDataBufferDecoder<ByteBuffer> {

  public ByteBufferDecoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return ByteBuffer.class.isAssignableFrom(elementType.toClass())
            && super.canDecode(elementType, mimeType);
  }

  @Override
  public ByteBuffer decode(
          DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    int len = dataBuffer.readableByteCount();
    ByteBuffer result = ByteBuffer.allocate(len);
    dataBuffer.toByteBuffer(result);
    if (logger.isDebugEnabled()) {
      logger.debug(Hints.getLogPrefix(hints) + "Read " + len + " bytes");
    }
    DataBufferUtils.release(dataBuffer);
    return result;
  }

}
