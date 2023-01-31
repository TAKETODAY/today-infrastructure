/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.codec;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.Netty5DataBuffer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
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
    super(MimeTypeUtils.ALL);
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
      logger.debug(Hints.getLogPrefix(hints) + "Read " + dataBuffer.readableByteCount() + " bytes");
    }
    if (dataBuffer instanceof Netty5DataBuffer netty5DataBuffer) {
      return netty5DataBuffer.getNativeBuffer();
    }
    byte[] bytes = new byte[dataBuffer.readableByteCount()];
    dataBuffer.read(bytes);
    Buffer buffer = DefaultBufferAllocators.preferredAllocator().copyOf(bytes);
    DataBufferUtils.release(dataBuffer);
    return buffer;
  }

}
