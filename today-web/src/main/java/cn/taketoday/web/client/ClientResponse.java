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

package cn.taketoday.web.client;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Nullable;

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
  <T> T bodyTo(Class<T> bodyType);

  /**
   * Extract the response body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body, or {@code null} if no response body was available
   */
  @Nullable
  <T> T bodyTo(ParameterizedTypeReference<T> bodyType);
}
