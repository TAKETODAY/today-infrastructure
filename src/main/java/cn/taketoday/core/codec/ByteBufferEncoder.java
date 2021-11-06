/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Encoder for {@link ByteBuffer ByteBuffers}.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class ByteBufferEncoder extends AbstractEncoder<ByteBuffer> {

  public ByteBufferEncoder() {
    super(MimeTypeUtils.ALL);
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    Class<?> clazz = elementType.toClass();
    return super.canEncode(elementType, mimeType) && ByteBuffer.class.isAssignableFrom(clazz);
  }

  @Override
  public Flux<DataBuffer> encode(
          Publisher<? extends ByteBuffer> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(inputStream)
            .map(byteBuffer -> encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
  }

  @Override
  public DataBuffer encodeValue(
          ByteBuffer byteBuffer, DataBufferFactory bufferFactory,
          ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    DataBuffer dataBuffer = bufferFactory.wrap(byteBuffer);
    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      logger.debug("{}Writing {} bytes", Hints.getLogPrefix(hints), dataBuffer.readableByteCount());
    }
    return dataBuffer;
  }

}
