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
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Abstract base class for {@link cn.taketoday.core.codec.Encoder}
 * classes that can only deal with a single value.
 *
 * @param <T> the element type
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class AbstractSingleValueEncoder<T> extends AbstractEncoder<T> {

  public AbstractSingleValueEncoder(MimeType... supportedMimeTypes) {
    super(supportedMimeTypes);
  }

  @Override
  public final Flux<DataBuffer> encode(
          Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(inputStream)
            .take(1)
            .concatMap(value -> encode(value, bufferFactory, elementType, mimeType, hints))
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
  }

  /**
   * Encode {@code T} to an output {@link DataBuffer} stream.
   *
   * @param t the value to process
   * @param dataBufferFactory a buffer factory used to create the output
   * @param type the stream element type to process
   * @param mimeType the mime type to process
   * @param hints additional information about how to do decode, optional
   * @return the output stream
   */
  protected abstract Flux<DataBuffer> encode(
          T t, DataBufferFactory dataBufferFactory, ResolvableType type,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

}
