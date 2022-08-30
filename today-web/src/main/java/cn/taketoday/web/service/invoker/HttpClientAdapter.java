/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.service.invoker;

import cn.taketoday.core.TypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contract to abstract the underlying HTTP client and decouple it from the
 * {@linkplain HttpServiceProxyFactory#createClient(Class) HTTP service proxy}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface HttpClientAdapter {

  /**
   * Perform the given request, and release the response content, if any.
   *
   * @param requestValues the request to perform
   * @return {@code Mono} that completes when the request is fully executed
   * and the response content is released.
   */
  Mono<Void> requestToVoid(HttpRequestValues requestValues);

  /**
   * Perform the given request, release the response content, and return the
   * response headers.
   *
   * @param requestValues the request to perform
   * @return {@code Mono} that returns the response headers the request is
   * fully executed and the response content released.
   */
  Mono<HttpHeaders> requestToHeaders(HttpRequestValues requestValues);

  /**
   * Perform the given request and decode the response content to the given type.
   *
   * @param requestValues the request to perform
   * @param bodyType the target type to decode to
   * @param <T> the type the response is decoded to
   * @return {@code Mono} that returns the decoded response.
   */
  <T> Mono<T> requestToBody(HttpRequestValues requestValues, TypeReference<T> bodyType);

  /**
   * Perform the given request and decode the response content to a stream with
   * elements of the given type.
   *
   * @param requestValues the request to perform
   * @param bodyType the target stream element type to decode to
   * @param <T> the type the response is decoded to
   * @return {@code Flux} with decoded stream elements.
   */
  <T> Flux<T> requestToBodyFlux(HttpRequestValues requestValues, TypeReference<T> bodyType);

  /**
   * Variant of {@link #requestToVoid(HttpRequestValues)} with additional
   * access to the response status and headers.
   */
  Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestValues requestValues);

  /**
   * Variant of {@link #requestToBody(HttpRequestValues, TypeReference)}
   * with additional access to the response status and headers.
   */
  <T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestValues requestValues, TypeReference<T> bodyType);

  /**
   * Variant of {@link #requestToBodyFlux(HttpRequestValues, TypeReference)}
   * with additional access to the response status and headers.
   */
  <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestValues requestValues, TypeReference<T> bodyType);

}
