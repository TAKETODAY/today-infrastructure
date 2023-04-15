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

import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Strategy for decoding a {@link DataBuffer} input stream into an output stream
 * of elements of type {@code <T>}.
 *
 * @param <T> the type of elements in the output stream
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface Decoder<T> {

  /**
   * Whether the decoder supports the given target element type and the MIME
   * type of the source stream.
   *
   * @param elementType the target element type for the output stream
   * @param mimeType the mime type associated with the stream to decode
   * (can be {@code null} if not specified)
   * @return {@code true} if supported, {@code false} otherwise
   */
  boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType);

  /**
   * Decode a {@link DataBuffer} input stream into a Flux of {@code T}.
   *
   * @param inputStream the {@code DataBuffer} input stream to decode
   * @param elementType the expected type of elements in the output stream;
   * this type must have been previously passed to the {@link #canDecode}
   * method and it must have returned {@code true}.
   * @param mimeType the MIME type associated with the input stream (optional)
   * @param hints additional information about how to do decode
   * @return the output stream with decoded elements
   */
  Flux<T> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

  /**
   * Decode a {@link DataBuffer} input stream into a Mono of {@code T}.
   *
   * @param inputStream the {@code DataBuffer} input stream to decode
   * @param elementType the expected type of elements in the output stream;
   * this type must have been previously passed to the {@link #canDecode}
   * method and it must have returned {@code true}.
   * @param mimeType the MIME type associated with the input stream (optional)
   * @param hints additional information about how to do decode
   * @return the output stream with the decoded element
   */
  Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

  /**
   * Decode a data buffer to an Object of type T. This is useful for scenarios,
   * that distinct messages (or events) are decoded and handled individually,
   * in fully aggregated form.
   *
   * @param buffer the {@code DataBuffer} to decode
   * @param targetType the expected output type
   * @param mimeType the MIME type associated with the data
   * @param hints additional information about how to do decode
   * @return the decoded value, possibly {@code null}
   */
  @Nullable
  default T decode(DataBuffer buffer, ResolvableType targetType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

    CompletableFuture<T> future = decodeToMono(Mono.just(buffer), targetType, mimeType, hints).toFuture();
    Assert.state(future.isDone(), "DataBuffer decoding should have completed.");

    Throwable failure;
    try {
      return future.get();
    }
    catch (ExecutionException ex) {
      failure = ex.getCause();
    }
    catch (InterruptedException ex) {
      failure = ex;
    }
    throw failure instanceof CodecException ? (CodecException) failure :
          new DecodingException("Failed to decode: " + failure.getMessage(), failure);
  }

  /**
   * Return the list of MIME types supported by this Decoder. The list may not
   * apply to every possible target element type and calls to this method
   * should typically be guarded via {@link #canDecode(ResolvableType, MimeType)
   * canDecode(elementType, null)}. The list may also exclude MIME types
   * supported only for a specific element type. Alternatively, use
   * {@link #getDecodableMimeTypes(ResolvableType)} for a more precise list.
   *
   * @return the list of supported MIME types
   */
  List<MimeType> getDecodableMimeTypes();

  /**
   * Return the list of MIME types supported by this Decoder for the given type
   * of element. This list may differ from {@link #getDecodableMimeTypes()}
   * if the Decoder doesn't support the given element type or if it supports
   * it only for a subset of MIME types.
   *
   * @param targetType the type of element to check for decoding
   * @return the list of MIME types supported for the given target type
   */
  default List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
    return canDecode(targetType, null) ? getDecodableMimeTypes() : Collections.emptyList();
  }

}
