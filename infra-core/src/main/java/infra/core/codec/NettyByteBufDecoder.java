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

package infra.core.codec;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.NettyDataBuffer;
import infra.util.MimeType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Decoder for {@link ByteBuf ByteBufs}.
 *
 * @author Vladislav Kisel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class NettyByteBufDecoder extends AbstractDataBufferDecoder<ByteBuf> {

  public NettyByteBufDecoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return ByteBuf.class.isAssignableFrom(elementType.toClass())
            && super.canDecode(elementType, mimeType);
  }

  @Override
  public ByteBuf decode(DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (logger.isDebugEnabled()) {
      logger.debug("{}Read {} bytes", Hints.getLogPrefix(hints), dataBuffer.readableBytes());
    }
    if (dataBuffer instanceof NettyDataBuffer) {
      return ((NettyDataBuffer) dataBuffer).getNativeBuffer();
    }
    byte[] bytes = new byte[dataBuffer.readableBytes()];
    dataBuffer.read(bytes);
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    dataBuffer.release();
    return byteBuf;
  }

}
