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

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;

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
   * Variant of {@link #exchangeForBody(HttpRequestValues, ParameterizedTypeReference)}
   * with additional access to the response status and headers.
   *
   * @return the response entity with status, headers, and body.
   */
  <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

}
