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

package cn.taketoday.http.codec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Strategy for reading from a {@link ReactiveHttpInputMessage} and decoding
 * the stream of bytes to Objects of type {@code <T>}.
 *
 * @param <T> the type of objects in the decoded output stream
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpMessageReader<T> {

  /**
   * Return the list of media types supported by this reader. The list may not
   * apply to every possible target element type and calls to this method
   * should typically be guarded via {@link #canRead(ResolvableType, MediaType)
   * canWrite(elementType, null)}. The list may also exclude media types
   * supported only for a specific element type. Alternatively, use
   * {@link #getReadableMediaTypes(ResolvableType)} for a more precise list.
   *
   * @return the general list of supported media types
   */
  List<MediaType> getReadableMediaTypes();

  /**
   * Return the list of media types supported by this Reader for the given type
   * of element. This list may differ from {@link #getReadableMediaTypes()}
   * if the Reader doesn't support the element type, or if it supports it
   * only for a subset of media types.
   *
   * @param elementType the type of element to read
   * @return the list of media types supported for the given class
   */
  default List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
    return (canRead(elementType, null) ? getReadableMediaTypes() : Collections.emptyList());
  }

  /**
   * Whether the given object type is supported by this reader.
   *
   * @param elementType the type of object to check
   * @param mediaType the media type for the read (possibly {@code null})
   * @return {@code true} if readable, {@code false} otherwise
   */
  boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType);

  /**
   * Read from the input message and decode to a stream of objects.
   *
   * @param elementType the type of objects in the stream which must have been
   * previously checked via {@link #canRead(ResolvableType, MediaType)}
   * @param message the message to read from
   * @param hints additional information about how to read and decode the input
   * @return the decoded stream of elements
   */
  Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

  /**
   * Read from the input message and decode to a single object.
   *
   * @param elementType the type of objects in the stream which must have been
   * previously checked via {@link #canRead(ResolvableType, MediaType)}
   * @param message the message to read from
   * @param hints additional information about how to read and decode the input
   * @return the decoded object
   */
  Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

  /**
   * Server-side only alternative to
   * {@link #read(ResolvableType, ReactiveHttpInputMessage, Map)}
   * with additional context available.
   *
   * @param actualType the actual type of the target method parameter;
   * for annotated controllers, the {@link ResolvableMethodParameter} can be accessed
   * via {@link ResolvableType#getSource()}.
   * @param elementType the type of Objects in the output stream
   * @param request the current request
   * @param response the current response
   * @param hints additional information about how to read the body
   * @return the decoded stream of elements
   */
  default Flux<T> read(ResolvableType actualType, ResolvableType elementType,
          ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

    return read(elementType, request, hints);
  }

  /**
   * Server-side only alternative to
   * {@link #readMono(ResolvableType, ReactiveHttpInputMessage, Map)}
   * with additional, context available.
   *
   * @param actualType the actual type of the target method parameter;
   * for annotated controllers, the {@link ResolvableMethodParameter} can be accessed
   * via {@link ResolvableType#getSource()}.
   * @param elementType the type of Objects in the output stream
   * @param request the current request
   * @param response the current response
   * @param hints additional information about how to read the body
   * @return the decoded stream of elements
   */
  default Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
          ServerHttpResponse response, Map<String, Object> hints) {

    return readMono(elementType, request, hints);
  }

}
