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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import infra.core.ParameterizedTypeReference;
import infra.http.client.ClientHttpResponse;

/**
 * Represents an HTTP response, as returned by {@link RestClient}.
 * Provides access to the response status and headers, and also
 * methods to consume the response body.
 *
 * <p> Extension of {@link ClientHttpResponse} that can convert the body.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/29 17:39
 */
public interface ClientResponse extends ClientHttpResponse {

  /**
   * Extract the response body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body, or {@code null} if no response body was available
   */
  @Nullable
  <T> T bodyTo(Class<T> bodyType) throws RestClientException;

  /**
   * Extract the response body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body, or {@code null} if no response body was available
   */
  @Nullable
  <T> T bodyTo(ParameterizedTypeReference<T> bodyType) throws RestClientException;
}
