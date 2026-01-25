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

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.ResponseEntity;
import infra.util.concurrent.Future;

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
   * Perform the given request, and release the response content, if any.
   *
   * @param requestValues the request to perform
   */
  void exchange(HttpRequestValues requestValues);

  /**
   * Perform the given request, release the response content, and return the
   * response headers.
   *
   * @param requestValues the request to perform
   * @return the response headers
   */
  HttpHeaders exchangeForHeaders(HttpRequestValues requestValues);

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

  /**
   * Creates an HTTP request executor for the given method and return type.
   *
   * @param method the method to create executor for
   * @return a new HTTP request executor instance
   */
  default @Nullable RequestExecution<HttpRequestValues> createRequestExecution(Method method, MethodParameter returnType, boolean isFuture) {
    return null;
  }

}
