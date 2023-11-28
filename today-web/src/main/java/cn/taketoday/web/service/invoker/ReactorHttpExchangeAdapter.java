/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.time.Duration;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contract to abstract a reactive, HTTP client from
 * {@linkplain HttpServiceProxyFactory} and make it pluggable.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ReactorHttpExchangeAdapter extends HttpExchangeAdapter {

  /**
   * Return the configured {@link ReactiveAdapterRegistry}.
   */
  ReactiveAdapterRegistry getReactiveAdapterRegistry();

  /**
   * Return the configured time to block for the response from an HTTP service
   * method with a synchronous (blocking) method signature.
   *
   * <p>By default, not set in which case the behavior depends on connection
   * and request timeout settings of the underlying HTTP client. We recommend
   * configuring timeout values directly on the underlying HTTP client, which
   * provides more control over such settings.
   */
  @Nullable
  Duration getBlockTimeout();

  /**
   * Perform the given request, and release the response content, if any.
   *
   * @param requestValues the request to perform
   * @return {@code Mono} that completes when the request is fully executed
   * and the response content is released.
   */
  Mono<Void> exchangeForMono(HttpRequestValues requestValues);

  /**
   * Perform the given request, release the response content, and return the
   * response headers.
   *
   * @param requestValues the request to perform
   * @return {@code Mono} that returns the response headers the request is
   * fully executed and the response content released.
   */
  Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues);

  /**
   * Perform the given request and decode the response content to the given type.
   *
   * @param requestValues the request to perform
   * @param bodyType the target type to decode to
   * @param <T> the type the response is decoded to
   * @return {@code Mono} that returns the decoded response.
   */
  <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

  /**
   * Perform the given request and decode the response content to a stream with
   * elements of the given type.
   *
   * @param requestValues the request to perform
   * @param bodyType the target stream element type to decode to
   * @param <T> the type the response is decoded to
   * @return {@code Flux} with decoded stream elements.
   */
  <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

  /**
   * Variant of {@link #exchangeForMono(HttpRequestValues)} with additional
   * access to the response status and headers.
   */
  Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues);

  /**
   * Variant of {@link #exchangeForBodyMono(HttpRequestValues, ParameterizedTypeReference)}
   * with additional access to the response status and headers.
   */
  <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

  /**
   * Variant of {@link #exchangeForBodyFlux(HttpRequestValues, ParameterizedTypeReference)}
   * with additional access to the response status and headers.
   */
  <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

}
