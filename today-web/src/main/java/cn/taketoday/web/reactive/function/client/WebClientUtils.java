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

package cn.taketoday.web.reactive.function.client;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.core.codec.CodecException;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Constant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Internal methods shared between {@link DefaultWebClient} and
 * {@link DefaultClientResponse}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class WebClientUtils {

  /**
   * Predicate that returns true if an exception should be wrapped.
   */
  public final static Predicate<? super Throwable> WRAP_EXCEPTION_PREDICATE =
          t -> !(t instanceof WebClientException) && !(t instanceof CodecException);

  /**
   * Map the given response to a single value {@code ResponseEntity<T>}.
   */
  @SuppressWarnings("unchecked")
  public static <T> Mono<ResponseEntity<T>> mapToEntity(ClientResponse response, Mono<T> bodyMono) {
    return ((Mono<Object>) bodyMono).defaultIfEmpty(Constant.DEFAULT_NONE).map(body ->
            new ResponseEntity<>(
                    body != Constant.DEFAULT_NONE ? (T) body : null,
                    response.headers().asHttpHeaders(),
                    response.statusCode()));
  }

  /**
   * Map the given response to a {@code ResponseEntity<List<T>>}.
   */
  public static <T> Mono<ResponseEntity<List<T>>> mapToEntityList(ClientResponse response, Publisher<T> body) {
    return Flux.from(body)
            .collectList()
            .map(list -> new ResponseEntity<>(
                    list, response.headers().asHttpHeaders(), response.statusCode()));
  }

}
