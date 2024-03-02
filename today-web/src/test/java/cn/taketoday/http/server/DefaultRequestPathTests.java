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

package cn.taketoday.http.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultRequestPath}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultRequestPathTests {

  @Test
  void parse() {
    // basic
    testParse("/app/a/b/c", "/app", "/a/b/c");

    // no context path
    testParse("/a/b/c", "", "/a/b/c");

    // context path only
    testParse("/a/b", "/a/b", "");

    // root path
    testParse("/", "", "/");

    // empty path
    testParse("", "", "");
    testParse("", "/", "");

    // trailing slash
    testParse("/app/a/", "/app", "/a/");
    testParse("/app/a//", "/app", "/a//");
  }

  private void testParse(String fullPath, String contextPath, String pathWithinApplication) {
    RequestPath requestPath = RequestPath.parse(fullPath, contextPath);
    Object expected = contextPath.equals("/") ? "" : contextPath;
    assertThat(requestPath.contextPath().value()).isEqualTo(expected);
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo(pathWithinApplication);
  }

  @Test
  void modifyContextPath() {
    RequestPath requestPath = RequestPath.parse("/aA/bB/cC", null);

    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/aA/bB/cC");

    requestPath = requestPath.modifyContextPath("/aA");

    assertThat(requestPath.contextPath().value()).isEqualTo("/aA");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/bB/cC");

    requestPath = requestPath.modifyContextPath(null);

    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/aA/bB/cC");
  }

}
