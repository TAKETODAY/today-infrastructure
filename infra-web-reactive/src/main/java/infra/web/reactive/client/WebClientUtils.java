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

package infra.web.reactive.client;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.function.Predicate;

import infra.core.codec.CodecException;
import infra.http.ResponseEntity;
import infra.lang.Constant;
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
  public static final Predicate<? super Throwable> WRAP_EXCEPTION_PREDICATE =
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
