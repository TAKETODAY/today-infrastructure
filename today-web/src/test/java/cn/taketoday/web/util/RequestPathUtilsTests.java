/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.server.RequestPath;
import cn.taketoday.web.servlet.MockServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 22:14
 */
class RequestPathUtilsTests {

  @Test
  void parseAndCache() {
    // basic
    testParseAndCache("/app/a/b/c", "/app", "/a/b/c");

    // contextPath only, servletPathOnly, contextPath and servletPathOnly
    testParseAndCache("/app/a/b/c", "/app", "/a/b/c");
    testParseAndCache("/a/b/c", "", "/a/b/c");
    testParseAndCache("/app1/app2", "/app1/app2", "");

    // trailing slash
    testParseAndCache("/app/a/", "/app", "/a/");
    testParseAndCache("/app/a//", "/app", "/a//");
  }

  private void testParseAndCache(
          String requestUri, String contextPath, String pathWithinApplication) {

    MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
    request.setContextPath(contextPath);
    MockServletRequestContext context = new MockServletRequestContext(request, null);
    RequestPath requestPath = context.getRequestPath();

    assertThat(requestPath.contextPath().value()).isEqualTo(contextPath);
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
  }

}
