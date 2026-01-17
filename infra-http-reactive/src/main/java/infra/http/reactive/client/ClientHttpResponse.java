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

package infra.http.reactive.client;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.http.reactive.ReactiveHttpInputMessage;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;

/**
 * Represents a client-side reactive HTTP response.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ClientHttpResponse extends ReactiveHttpInputMessage {

  /**
   * Return an id that represents the underlying connection, if available,
   * or the request for the purpose of correlating log messages.
   */
  default String getId() {
    return ObjectUtils.getIdentityHexString(this);
  }

  /**
   * Return the HTTP status code as an {@link HttpStatus} enum value.
   *
   * @return the HTTP status as an HttpStatus enum value (never {@code null})
   * @throws IllegalArgumentException in case of an unknown HTTP status code
   * @see HttpStatus#valueOf(int)
   * @since #getRawStatusCode()
   */
  default HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(getRawStatusCode());
  }

  /**
   * Return the HTTP status code (potentially non-standard and not
   * resolvable through the {@link HttpStatus} enum) as an integer.
   *
   * @return the HTTP status as an integer value
   * @see #getStatusCode()
   * @see HttpStatus#resolve(int)
   */
  int getRawStatusCode();

  /**
   * Return a read-only map of response cookies received from the server.
   */
  MultiValueMap<String, ResponseCookie> getCookies();

}
