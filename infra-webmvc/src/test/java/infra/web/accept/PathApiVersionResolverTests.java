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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import infra.http.server.PathContainer;
import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void resolveWithVersionPathPredicate() {
    testVersionPathPredicate("/app/1.0/path", "1.0");
    testVersionPathPredicate("/app", null);
    testVersionPathPredicate("/v3/api-docs", null);
  }

  private static void testVersionPathPredicate(String requestUri, String expectedVersion) {
    Predicate<RequestPath> versionPathPredicate = path -> {
      List<PathContainer.Element> elements = path.elements();
      return (elements.size() > 3 &&
              elements.get(1).value().equals("app") &&
              elements.get(3).value().matches("\\d+\\.\\d+"));
    };

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", requestUri);
    MockRequestContext context = new MockRequestContext(null, request, null);

    PathApiVersionResolver resolver = new PathApiVersionResolver(1, versionPathPredicate);
    String actual = resolver.resolveVersion(context);
    Assertions.assertThat(actual).isEqualTo(expectedVersion);
  }

  private static void testResolve(int index, String requestUri, String expected) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", requestUri);
    MockRequestContext context = new MockRequestContext(null, request, null);

    String actual = new PathApiVersionResolver(index).resolveVersion(context);
    Assertions.assertThat(actual).isEqualTo(expected);
  }

}