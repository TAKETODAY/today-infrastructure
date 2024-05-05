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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.server.RequestPath;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.web.MockHttpServletRequest;

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
    MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
    ServletRequestContext context = new ServletRequestContext(null, request, null);
    RequestPath requestPath = context.getRequestPath();

    assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
  }

}
