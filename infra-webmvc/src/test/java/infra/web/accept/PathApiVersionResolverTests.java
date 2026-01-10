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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:24
 */
class PathApiVersionResolverTests {

  @Test
  void resolve() {
    testResolve(0, "/1.0/path", "1.0");
    testResolve(1, "/app/1.1/path", "1.1");
  }

  @Test
  void insufficientPathSegments() {
    assertThatThrownBy(() -> testResolve(0, "/", "1.0")).isInstanceOf(InvalidApiVersionException.class);
  }

  @Test
  void constructorWithNegativeIndexThrowsException() {
    assertThatThrownBy(() -> new PathApiVersionResolver(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'pathSegmentIndex' must be >= 0");
  }

  @Test
  void constructorWithZeroIndex() {
    PathApiVersionResolver resolver = new PathApiVersionResolver(0);
    assertThat(resolver).isNotNull();
  }

  @Test
  void resolveVersionWithExactSegmentMatch() {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v2/users");
    MockRequestContext context = new MockRequestContext(null, request, null);

    PathApiVersionResolver resolver = new PathApiVersionResolver(1);
    String version = resolver.resolveVersion(context);

    assertThat(version).isEqualTo("v2");
  }

  @Test
  void resolveVersionWithFirstSegment() {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/v1/products/123");
    MockRequestContext context = new MockRequestContext(null, request, null);

    PathApiVersionResolver resolver = new PathApiVersionResolver(0);
    String version = resolver.resolveVersion(context);

    assertThat(version).isEqualTo("v1");
  }

  @Test
  void resolveVersionWithEncodedPathSegments() {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v3%2E0/resource");
    MockRequestContext context = new MockRequestContext(null, request, null);

    PathApiVersionResolver resolver = new PathApiVersionResolver(1);
    String version = resolver.resolveVersion(context);

    assertThat(version).isEqualTo("v3%2E0");
  }

  @Test
  void resolveVersionThrowsExceptionWhenIndexOutOfBounds() {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api");
    MockRequestContext context = new MockRequestContext(null, request, null);

    PathApiVersionResolver resolver = new PathApiVersionResolver(2);

    assertThatThrownBy(() -> resolver.resolveVersion(context))
            .isInstanceOf(InvalidApiVersionException.class)
            .hasMessage("400 BAD_REQUEST \"Invalid API version: 'No path segment at index 2'.\"");
  }

  private static void testResolve(int index, String requestUri, String expected) {
    HttpMockRequest request = new HttpMockRequestImpl("GET", requestUri);
    String actual = new PathApiVersionResolver(index).resolveVersion(
            new MockRequestContext(null, request, null));
    assertThat(actual).isEqualTo(expected);
  }

}