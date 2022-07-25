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

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Simple pass-through decoder for {@link DataBuffer DataBuffers}.
 *
 * <p><strong>Note:</strong> The data buffers should be released via
 * {@link cn.taketoday.core.io.buffer.DataBufferUtils#release(DataBuffer)}
 * after they have been consumed. In addition, if using {@code Flux} or
 * {@code Mono} operators such as flatMap, reduce, and others that prefetch,
 * cache, and skip or filter out data items internally, please add
 * {@code doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release)} to the
 * composition chain to ensure cached data buffers are released prior to an
 * error or cancellation signal.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DataBufferDecoder extends AbstractDataBufferDecoder<DataBuffer> {

  public DataBufferDecoder() {
    super(MimeTypeUtils.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return DataBuffer.class.isAssignableFrom(elementType.toClass())
            && super.canDecode(elementType, mimeType);
  }

  @Override
  public Flux<DataBuffer> decode(
          Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(input);
  }

  @Override
  public DataBuffer decode(
          DataBuffer buffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    if (logger.isDebugEnabled()) {
      logger.debug("{}Read {} bytes", Hints.getLogPrefix(hints), buffer.readableByteCount());
    }
    return buffer;
  }

}
