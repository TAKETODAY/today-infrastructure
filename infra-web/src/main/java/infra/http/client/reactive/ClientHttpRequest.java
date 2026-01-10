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

package infra.http.client.reactive;

import java.net.URI;

import infra.core.AttributeAccessor;
import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.http.ReactiveHttpOutputMessage;
import infra.util.MultiValueMap;

/**
 * Represents a client-side reactive HTTP request.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ClientHttpRequest extends ReactiveHttpOutputMessage, AttributeAccessor {

  /**
   * Return the HTTP method of the request.
   */
  HttpMethod getMethod();

  /**
   * Return the URI of the request.
   */
  URI getURI();

  /**
   * Return a mutable map of request cookies to send to the server.
   */
  MultiValueMap<String, HttpCookie> getCookies();

  /**
   * Return the request from the underlying HTTP library.
   *
   * @param <T> the expected type of the request to cast to
   */
  <T> T getNativeRequest();

}
