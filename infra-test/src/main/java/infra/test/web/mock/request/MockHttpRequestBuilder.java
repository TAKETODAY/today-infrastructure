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

package infra.test.web.mock.request;

import java.net.URI;

import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.mock.web.HttpMockRequestImpl;
import infra.test.web.mock.MockMvc;
import infra.web.util.UriComponentsBuilder;

/**
 * Default builder for {@link HttpMockRequestImpl} required as input to
 * perform requests in {@link MockMvc}.
 *
 * <p>Application tests will typically access this builder through the static
 * factory methods in {@link MockMvcRequestBuilders}.
 *
 * <p>This class is not open for extension. To apply custom initialization to
 * the created {@code MockHttpMockRequest}, please use the
 * {@link #with(RequestPostProcessor)} extension point.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockHttpRequestBuilder extends AbstractMockHttpServletRequestBuilder<MockHttpRequestBuilder> {

  /**
   * Package private constructor. To get an instance, use static factory
   * methods in {@link MockMvcRequestBuilders}.
   * <p>Although this class cannot be extended, additional ways to initialize
   * the {@code MockHttpServletRequest} can be plugged in via
   * {@link #with(RequestPostProcessor)}.
   *
   * @param httpMethod the HTTP method (GET, POST, etc.)
   */
  MockHttpRequestBuilder(HttpMethod httpMethod) {
    super(httpMethod);
  }

  /**
   * Package private constructor. To get an instance, use static factory
   * methods in {@link MockMvcRequestBuilders}.
   * <p>Although this class cannot be extended, additional ways to initialize
   * the {@code MockHttpMockRequest} can be plugged in via
   * {@link #with(RequestPostProcessor)}.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param url a URL template; the resulting URL will be encoded
   * @param vars zero or more URI variables
   */
  MockHttpRequestBuilder(HttpMethod httpMethod, String url, Object... vars) {
    this(httpMethod, initUri(url, vars));
  }

  private static URI initUri(String url, Object[] vars) {
    Assert.notNull(url, "'url' is required");
    Assert.isTrue(url.isEmpty() || url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://"),
            () -> "'url' should start with a path or be a complete HTTP URL: " + url);
    String uriString = (url.isEmpty() ? "/" : url);
    return UriComponentsBuilder.forURIString(uriString).buildAndExpand(vars).encode().toURI();
  }

  /**
   * Alternative to {@link #MockHttpRequestBuilder(HttpMethod, String, Object...)}
   * with a pre-built URI.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param url the URL
   */
  MockHttpRequestBuilder(HttpMethod httpMethod, URI url) {
    this(httpMethod);
    uri(url);
  }

  /**
   * Alternative constructor for custom HTTP methods.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param url the URL
   */
  MockHttpRequestBuilder(String httpMethod, URI url) {
    super(HttpMethod.valueOf(httpMethod));
    uri(url);
  }

}
