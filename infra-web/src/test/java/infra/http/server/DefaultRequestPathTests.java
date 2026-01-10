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

package infra.http.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  @Test
  void parse_basic() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/app");

    assertThat(requestPath.value()).isEqualTo("/app/a/b/c");
    assertThat(requestPath.contextPath().value()).isEqualTo("/app");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/a/b/c");
  }

  @Test
  void parse_noContextPath() {
    RequestPath requestPath = RequestPath.parse("/a/b/c", "");

    assertThat(requestPath.value()).isEqualTo("/a/b/c");
    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/a/b/c");
  }

  @Test
  void parse_contextPathOnly() {
    RequestPath requestPath = RequestPath.parse("/a/b", "/a/b");

    assertThat(requestPath.value()).isEqualTo("/a/b");
    assertThat(requestPath.contextPath().value()).isEqualTo("/a/b");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("");
  }

  @Test
  void parse_rootPath() {
    RequestPath requestPath = RequestPath.parse("/", "");

    assertThat(requestPath.value()).isEqualTo("/");
    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/");
  }

  @Test
  void parse_emptyPath() {
    RequestPath requestPath = RequestPath.parse("", "");

    assertThat(requestPath.value()).isEqualTo("");
    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("");
  }

  @Test
  void parse_withSlashContextPath() {
    RequestPath requestPath = RequestPath.parse("", "/");

    assertThat(requestPath.value()).isEqualTo("");
    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("");
  }

  @Test
  void parse_trailingSlash() {
    RequestPath requestPath = RequestPath.parse("/app/a/", "/app");

    assertThat(requestPath.value()).isEqualTo("/app/a/");
    assertThat(requestPath.contextPath().value()).isEqualTo("/app");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/a/");
  }

  @Test
  void parse_doubleTrailingSlash() {
    RequestPath requestPath = RequestPath.parse("/app/a//", "/app");

    assertThat(requestPath.value()).isEqualTo("/app/a//");
    assertThat(requestPath.contextPath().value()).isEqualTo("/app");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/a//");
  }

  @Test
  void modifyContextPath_fromNullToValue() {
    RequestPath requestPath = RequestPath.parse("/aA/bB/cC", null);

    assertThat(requestPath.contextPath().value()).isEqualTo("");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/aA/bB/cC");

    RequestPath modifiedPath = requestPath.modifyContextPath("/aA");

    assertThat(modifiedPath.contextPath().value()).isEqualTo("/aA");
    assertThat(modifiedPath.pathWithinApplication().value()).isEqualTo("/bB/cC");
  }

  @Test
  void modifyContextPath_fromValueToNull() {
    RequestPath requestPath = RequestPath.parse("/aA/bB/cC", "/aA");

    assertThat(requestPath.contextPath().value()).isEqualTo("/aA");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/bB/cC");

    RequestPath modifiedPath = requestPath.modifyContextPath(null);

    assertThat(modifiedPath.contextPath().value()).isEqualTo("");
    assertThat(modifiedPath.pathWithinApplication().value()).isEqualTo("/aA/bB/cC");
  }

  @Test
  void modifyContextPath_changeValue() {
    RequestPath requestPath = RequestPath.parse("/aA/bB/cC", "/aA");
    RequestPath modifiedPath = requestPath.modifyContextPath("/aA/bB");

    assertThat(modifiedPath.contextPath().value()).isEqualTo("/aA/bB");
    assertThat(modifiedPath.pathWithinApplication().value()).isEqualTo("/cC");
  }

  @Test
  void equals_sameInstance() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/app");
    assertThat(requestPath).isEqualTo(requestPath);
  }

  @Test
  void equals_differentInstancesSameValues() {
    RequestPath path1 = RequestPath.parse("/app/a/b/c", "/app");
    RequestPath path2 = RequestPath.parse("/app/a/b/c", "/app");

    assertThat(path1).isEqualTo(path2);
    assertThat(path2).isEqualTo(path1);
  }

  @Test
  void equals_differentFullPath() {
    RequestPath path1 = RequestPath.parse("/app/a/b/c", "/app");
    RequestPath path2 = RequestPath.parse("/app/a/b/d", "/app");

    assertThat(path1).isNotEqualTo(path2);
  }

  @Test
  void equals_differentContextPath() {
    RequestPath path1 = RequestPath.parse("/app/a/b/c", "/app");
    RequestPath path2 = RequestPath.parse("/app/a/b/c", "/app/a");

    assertThat(path1).isNotEqualTo(path2);
  }

  @Test
  void equals_null() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/app");
    assertThat(requestPath).isNotEqualTo(null);
  }

  @Test
  void equals_differentClass() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/app");
    assertThat(requestPath).isNotEqualTo("string");
  }

  @Test
  void hashCode_consistentWithEquals() {
    RequestPath path1 = RequestPath.parse("/app/a/b/c", "/app");
    RequestPath path2 = RequestPath.parse("/app/a/b/c", "/app");

    assertThat(path1.hashCode()).isEqualTo(path2.hashCode());
  }

  @Test
  void toString_shouldReturnFullPath() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/app");
    assertThat(requestPath.toString()).isEqualTo("/app/a/b/c");
  }

  @Test
  void value_shouldReturnFullPathString() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/app");

    assertThat(requestPath.value()).isEqualTo("/app/a/b/c");
    assertThat(requestPath.contextPath().value()).isEqualTo("/app");
    assertThat(requestPath.pathWithinApplication().value()).isEqualTo("/a/b/c");
  }

  @Test
  void initContextPath_withNullContextPath_shouldReturnEmpty() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", null);
    assertThat(requestPath.contextPath().value()).isEqualTo("");
  }

  @Test
  void initContextPath_withBlankContextPath_shouldReturnEmpty() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "   ");
    assertThat(requestPath.contextPath().value()).isEqualTo("");
  }

  @Test
  void initContextPath_withRootContextPath_shouldReturnEmpty() {
    RequestPath requestPath = RequestPath.parse("/app/a/b/c", "/");
    assertThat(requestPath.contextPath().value()).isEqualTo("");
  }

  @Test
  void validateContextPath_invalidStart_shouldThrowException() {
    assertThatThrownBy(() -> RequestPath.parse("/app/a/b/c", "app"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must start with '/'");
  }

  @Test
  void validateContextPath_invalidEnd_shouldThrowException() {
    assertThatThrownBy(() -> RequestPath.parse("/app/a/b/c", "/app/"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not end with '/'");
  }

  @Test
  void validateContextPath_notMatchingStart_shouldThrowException() {
    assertThatThrownBy(() -> RequestPath.parse("/app/a/b/c", "/xyz"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must match the start of requestPath");
  }

  @Test
  void validateContextPath_notMatchingFullSegment_shouldThrowException() {
    assertThatThrownBy(() -> RequestPath.parse("/application/a/b/c", "/app"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must match to full path segments");
  }

}
