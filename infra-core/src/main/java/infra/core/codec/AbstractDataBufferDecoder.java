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
import org.reactivestreams.Publisher;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.util.MimeType;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * {@link Resource Resource}, {@code String}, etc.
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

  @SuppressWarnings("NullAway")
  @Override
  public Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    return Flux.from(input)
            .mapNotNull(buffer -> decode(buffer, elementType, mimeType, hints));
  }

  @Override
  @SuppressWarnings("NullAway")
  public Mono<T> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return DataBufferUtils.join(input, this.maxInMemorySize)
            .mapNotNull(buffer -> decode(buffer, elementType, mimeType, hints));
  }

}
