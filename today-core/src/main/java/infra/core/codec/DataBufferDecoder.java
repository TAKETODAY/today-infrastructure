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
import infra.core.io.buffer.DataBufferUtils;
import infra.lang.Nullable;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Simple pass-through decoder for {@link DataBuffer DataBuffers}.
 *
 * <p><strong>Note:</strong> The data buffers should be released via
 * {@link DataBufferUtils#release(DataBuffer)}
 * after they have been consumed. In addition, if using {@code Flux} or
 * {@code Mono} operators such as flatMap, reduce, and others that prefetch,
 * cache, and skip or filter out data items internally, please add
 * {@code doOnDiscard(DataBuffer.class, DataBufferUtils::release)} to the
 * composition chain to ensure cached data buffers are released prior to an
 * error or cancellation signal.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class DataBufferDecoder extends AbstractDataBufferDecoder<DataBuffer> {

  public DataBufferDecoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return DataBuffer.class.isAssignableFrom(elementType.toClass())
            && super.canDecode(elementType, mimeType);
  }

  @Override
  public Flux<DataBuffer> decode(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(input);
  }

  @Override
  public DataBuffer decode(DataBuffer buffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    if (logger.isDebugEnabled()) {
      logger.debug("{}Read {} bytes", Hints.getLogPrefix(hints), buffer.readableBytes());
    }
    return buffer;
  }

}
