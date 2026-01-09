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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import infra.core.ParameterizedTypeReference;
import infra.http.ResponseEntity;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;

/**
 * Contract to abstract an HTTP client from {@linkplain HttpServiceProxyFactory}
 * and make it pluggable.
 *
 * <p>For reactive clients, see {@link ReactorHttpExchangeAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpExchangeAdapter {

  /**
   * Whether the underlying client supports use of request attributes.
   */
  boolean supportsRequestAttributes();

  /**
   * Perform the given request, and release the response content, if any.
   *
   * @param requestValues the request to perform
   * @return Returns non-closed ConvertibleClientHttpResponse
   */
  ClientResponse exchange(HttpRequestValues requestValues);

  /**
   * Perform the given request, and release the response content, if any.
   *
   * @param requestValues the request to perform
   * @since 5.0
   */
  Future<ClientResponse> exchangeAsync(HttpRequestValues requestValues);

  /**
   * Perform the given request
   *
   * @param requestValues the request to perform
   * @since 5.0
   */
  <T> Future<T> exchangeAsyncBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyTypeRef);

  /**
   * Perform the given request, and release the response content, if any.
   *
   * @param requestValues the request to perform
   * @since 5.0
   */
  Future<Void> exchangeAsyncVoid(HttpRequestValues requestValues);

  /**
   * Perform the given request and decode the response content to the given type.
   *
   * @param requestValues the request to perform
   * @param bodyType the target type to decode to
   * @param <T> the type the response is decoded to
   * @return the decoded response body.
   */
  @Nullable
  <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

  /**
   * Variant of {@link #exchange(HttpRequestValues)} with additional
   * access to the response status and headers.
   *
   * @return the response entity with status and headers.
   */
  ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues);

  /**
   * Variant of {@link #exchange(HttpRequestValues)} with additional
   * access to the response status and headers.
   *
   * @return the response entity with status and headers.
   * @since 5.0
   */
  Future<ResponseEntity<Void>> exchangeForBodilessEntityAsync(HttpRequestValues requestValues);

  /**
   * Variant of {@link #exchangeForBody(HttpRequestValues, ParameterizedTypeReference)}
   * with additional access to the response status and headers.
   *
   * @return the response entity with status, headers, and body.
   */
  <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

  /**
   * Variant of {@link #exchangeForBody(HttpRequestValues, ParameterizedTypeReference)}
   * with additional access to the response status and headers.
   *
   * @return the response entity with status, headers, and body.
   * @since 5.0
   */
  <T> Future<ResponseEntity<T>> exchangeForEntityAsync(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

}
