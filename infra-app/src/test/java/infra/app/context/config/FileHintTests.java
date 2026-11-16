/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.context.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileHint}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class FileHintTests {

  @Test
  void shouldParseImplicitExtension() {
    var hint = FileHint.from("foo[.bar]");
    assertThat(hint.isPresent()).isTrue();
    assertThat(hint.getExtension()).isEqualTo(".bar");
    assertThat(FileHint.removeFrom("foo[.bar]")).isEqualTo("foo");
  }

  @Test
  void shouldParseExplicitExtension() {
    var hint = FileHint.from("foo[extension=.bar]");
    assertThat(hint.isPresent()).isTrue();
    assertThat(hint.getExtension()).isEqualTo(".bar");
    assertThat(FileHint.removeFrom("foo[extension=.bar]")).isEqualTo("foo");
  }

  @Test
  void shouldParseNestedBrackets() {
    var hint = FileHint.from("foo[encoding=[utf-8]]");
    assertThat(hint.isPresent()).isTrue();
    assertThat(hint.getEncoding()).isEqualTo("[utf-8]");
    assertThat(FileHint.removeFrom("foo[encoding=[utf-8]]")).isEqualTo("foo");
  }

  @Test
  void shouldParseEncoding() {
    var hint = FileHint.from("foo[encoding=utf-8]");
    assertThat(hint.isPresent()).isTrue();
    assertThat(hint.getEncoding()).isEqualTo("utf-8");
    assertThat(FileHint.removeFrom("foo[encoding=utf-8]")).isEqualTo("foo");
  }

  @Test
  void shouldParseAll() {
    var hint = FileHint.from("foo[extension=.bar][encoding=utf-8]");
    assertThat(hint.isPresent()).isTrue();
    assertThat(hint.getExtension()).isEqualTo(".bar");
    assertThat(hint.getEncoding()).isEqualTo("utf-8");
    assertThat(FileHint.removeFrom("foo[extension=.bar][encoding=utf-8]")).isEqualTo("foo");
  }

  @Test
  void shouldParseTrailingHintWhenValueContainsOtherBrackets() {
    assertThat(FileHint.from("foo[a]/bar[encoding=utf-8]").getEncoding()).isEqualTo("utf-8");
    assertThat(FileHint.removeFrom("foo[a]/bar[encoding=utf-8]")).isEqualTo("foo[a]/bar");
  }

  @Test
  void isPresentWhenHasNoHint() {
    assertThat(FileHint.from("foo").isPresent()).isFalse();
    assertThat(FileHint.from("foo[bar]").isPresent()).isFalse();
    assertThat(FileHint.from("foo[.b[ar]").isPresent()).isFalse();
  }

  @Test
  void getExtensionOrElseWhenHasHint() {
    assertThat(FileHint.from("foo[.bar]").getExtensionOrElse(".txt")).isEqualTo(".bar");
  }

  @Test
  void getExtensionOrElseWhenHasNoHint() {
    assertThat(FileHint.from("foo").getExtensionOrElse(".txt")).isEqualTo(".txt");
  }

  @ParameterizedTest
  @ValueSource(strings = { "foo[a]/bar.yaml", "foo[1].yaml", "file:/tmp/team[a]/app[1].yaml", "a[b]c[.yaml]d",
          "foo[.yaml]/", "foo]", "foo[a]]", "foo[", "[", "]" })
  void removeFromWhenBracketsAreNotTrailingKeepsValue(String value) {
    assertThat(FileHint.removeFrom(value)).isEqualTo(value);
    assertThat(FileHint.from(value).isPresent()).isFalse();
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = { "foo[.yaml]|foo", "foo[a]/bar[.yaml]|foo[a]/bar", "foo[a]|foo",
          "foo[a]bar[b]|foo[a]bar", "app.yaml[a][b]|app.yaml", "foo[.yaml][encoding=utf-8]|foo",
          "foo[encoding=[utf-8]]|foo", "foo[ .yaml ]|foo", "foo[[x]|foo[" })
  void removeFromWhenHasTrailingBracketsRemovesOnlyTheTrailingBrackets(String value, String expected) {
    assertThat(FileHint.removeFrom(value)).isEqualTo(expected);
  }

  @Test
  void removeFromWhenHasNoHint() {
    assertThat(FileHint.removeFrom("foo")).isEqualTo("foo");
  }

}
