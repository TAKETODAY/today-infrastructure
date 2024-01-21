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

import java.lang.reflect.Parameter;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;

/**
 * Extension of {@code Decoder} exposing extra methods relevant in the context
 * of HTTP request or response body decoding.
 *
 * @param <T> the type of elements in the output stream
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpMessageDecoder<T> extends Decoder<T> {

  /**
   * Get decoding hints based on the server request or annotations on the
   * target controller method parameter.
   *
   * @param actualType the actual target type to decode to, possibly a reactive
   * wrapper and sourced from {@link Parameter},
   * i.e. providing access to method parameter annotations
   * @param elementType the element type within {@code Flux/Mono} that we're
   * trying to decode to
   * @param request the current request
   * @param response the current response
   * @return a Map with hints, possibly empty
   */
  Map<String, Object> getDecodeHints(ResolvableType actualType,
          ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response);

}
