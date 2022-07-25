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
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for {@code Decoder} implementations that can decode
 * a {@code DataBuffer} directly to the target element type.
 *
 * <p>Sub-classes must implement {@link #decode} to provide a way to
 * transform a {@code DataBuffer} to the target data type. The default
 * {@link #decode} implementation transforms each individual data buffer while
 * {@link #decodeToMono} applies "reduce" and transforms the aggregated buffer.
 *
 * <p>Sub-classes can override {@link #decode} in order to split the input stream
 * along different boundaries (e.g. on new line characters for {@code String})
 * or always reduce to a single data buffer (e.g. {@code Resource}).
 *
 * @param <T> the element type
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {

  private int maxInMemorySize = 256 * 1024;

  protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
    super(supportedMimeTypes);
  }

  /**
   * Configure a limit on the number of bytes that can be buffered whenever
   * the input stream needs to be aggregated. This can be a result of
   * decoding to a single {@code DataBuffer},
   * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
   * {@link cn.taketoday.core.io.Resource Resource}, {@code String}, etc.
   * It can also occur when splitting the input stream, e.g. delimited text,
   * in which case the limit applies to data buffered between delimiters.
   * <p>By default this is set to 256K.
   *
   * @param byteCount the max number of bytes to buffer, or -1 for unlimited
   */
  public void setMaxInMemorySize(int byteCount) {
    this.maxInMemorySize = byteCount;
  }

  /**
   * Return the {@link #setMaxInMemorySize configured} byte count limit.
   */
  public int getMaxInMemorySize() {
    return this.maxInMemorySize;
  }

  @Override
  public Flux<T> decode(
          Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    return Flux.from(input)
            .mapNotNull(buffer -> decode(buffer, elementType, mimeType, hints));
  }

  @Override
  public Mono<T> decodeToMono(
          Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return DataBufferUtils.join(input, this.maxInMemorySize)
            .mapNotNull(buffer -> decode(buffer, elementType, mimeType, hints));
  }

}
