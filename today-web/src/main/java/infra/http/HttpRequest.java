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

package infra.http;

import java.net.URI;
import java.util.Map;

import infra.core.AttributeAccessor;

/**
 * Represents an HTTP request message, consisting of
 * {@linkplain #getMethod() method} and {@linkplain #getURI() uri}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpRequest extends HttpMessage, AttributeAccessor {

  /**
   * Return the HTTP method of the request.
   *
   * @return the HTTP method as an HttpMethod value
   * @see HttpMethod#resolve(String)
   */
  HttpMethod getMethod();

  /**
   * Return the HTTP method of the request as a String value.
   *
   * @return the HTTP method as a plain String
   * @see #getMethod()
   */
  default String getMethodValue() {
    return getMethod().name();
  }

  /**
   * Return the URI of the request (including a query string if any,
   * but only if it is well-formed for a URI representation).
   *
   * @return the URI of the request (never {@code null})
   */
  URI getURI();

  /**
   * Return a mutable map of request attributes for this request.
   *
   * @since 5.0
   */
  @Override
  Map<String, Object> getAttributes();

}
