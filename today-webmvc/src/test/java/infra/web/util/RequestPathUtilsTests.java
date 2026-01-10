/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.util;

import org.junit.jupiter.api.Test;

import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 22:14
 */
class RequestPathUtilsTests {

  @Test
  void parseAndCache() {
    // basic
    testParseAndCache("/a/b/c", "/a/b/c");

    // contextPath only, servletPathOnly, contextPath and servletPathOnly
    testParseAndCache("/a/b/c", "/a/b/c");
    testParseAndCache("/a/b/c", "/a/b/c");
    testParseAndCache("/", "/");

    // trailing slash
    testParseAndCache("/a/", "/a/");
    testParseAndCache("/a//", "/a//");
  }

  private void testParseAndCache(String requestUri, String pathWithinApplication) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", requestUri);
    MockRequestContext context = new MockRequestContext(null, request, null);
    RequestPath requestPath = context.getRequestPath();

    assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
  }

}
