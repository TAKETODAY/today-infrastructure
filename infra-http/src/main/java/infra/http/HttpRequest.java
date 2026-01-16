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
  default String getMethodAsString() {
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
