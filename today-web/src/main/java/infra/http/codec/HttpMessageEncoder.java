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

package infra.http.codec;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Encoder;
import infra.core.codec.Hints;
import infra.http.MediaType;
import infra.http.server.reactive.ServerHttpRequest;
import infra.http.server.reactive.ServerHttpResponse;
import infra.lang.Nullable;

/**
 * Extension of {@code Encoder} exposing extra methods relevant in the context
 * of HTTP request or response body encoding.
 *
 * @param <T> the type of elements in the input stream
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpMessageEncoder<T> extends Encoder<T> {

  /**
   * Return "streaming" media types for which flushing should be performed
   * automatically vs at the end of the input stream.
   */
  List<MediaType> getStreamingMediaTypes();

  /**
   * Get decoding hints based on the server request or annotations on the
   * target controller method parameter.
   *
   * @param actualType the actual source type to encode, possibly a reactive
   * wrapper and sourced from {@link Parameter},
   * i.e. providing access to method annotations.
   * @param elementType the element type within {@code Flux/Mono} that we're
   * trying to encode.
   * @param request the current request
   * @param response the current response
   * @return a Map with hints, possibly empty
   */
  default Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
          @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

    return Hints.none();
  }

}
