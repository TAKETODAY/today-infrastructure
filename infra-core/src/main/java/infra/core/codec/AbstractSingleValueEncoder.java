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
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Abstract base class for {@link Encoder}
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
            .doOnDiscard(DataBuffer.class, DataBuffer.RELEASE_CONSUMER);
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
