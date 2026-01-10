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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import infra.web.accept.SemanticApiVersionParser.Version;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:24
 */
class SemanticApiVersionParserTests {

  private final SemanticApiVersionParser parser = new SemanticApiVersionParser();

  @Test
  void compareVersionsWithSameMajor() {
    Version v1 = parser.parseVersion("1.2.3");
    Version v2 = parser.parseVersion("1.3.0");
    assertThat(v1.compareTo(v2)).isLessThan(0);
    assertThat(v1.equals(v2)).isFalse();
  }

  @Test
  void compareVersionsWithDifferentMajor() {
    Version v1 = parser.parseVersion("2.0.0");
    Version v2 = parser.parseVersion("1.9.9");
    assertThat(v1.compareTo(v2)).isGreaterThan(0);
    assertThat(v1.equals(v2)).isFalse();
  }

  @Test
  void compareVersionsWithSamePatchDifferentMinor() {
    Version v1 = parser.parseVersion("1.2.3");
    Version v2 = parser.parseVersion("1.1.3");
    assertThat(v1.compareTo(v2)).isGreaterThan(0);
    assertThat(v1.equals(v2)).isFalse();
  }

  @Test
  void equalVersionsHaveSameHashCode() {
    Version v1 = parser.parseVersion("1.2.3");
    Version v2 = parser.parseVersion("1.2.3");
    assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
    assertThat(v1.equals(v2)).isTrue();
  }

  @Test
  void versionToString() {
    Version version = parser.parseVersion("1.2.3");
    assertThat(version.toString()).isEqualTo("1.2.3");
  }

  @Test
  void parseNullVersion() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> parser.parseVersion(null))
            .withMessage("'version' is required");
  }

  @Test
  void skipMultipleNonDigitCharacters() {
    Version version = parser.parseVersion("version-v1.2.3");
    assertThat(version.major).isEqualTo(1);
    assertThat(version.minor).isEqualTo(2);
    assertThat(version.patch).isEqualTo(3);
  }

  @Test
  void parse() {
    testParse("0", 0, 0, 0);
    testParse("0.3", 0, 3, 0);
    testParse("4.5", 4, 5, 0);
    testParse("6.7.8", 6, 7, 8);
    testParse("v01", 1, 0, 0);
    testParse("version-1.2", 1, 2, 0);
  }

  private void testParse(String input, int major, int minor, int patch) {
    Version actual = this.parser.parseVersion(input);
    assertThat(actual.major).isEqualTo(major);
    assertThat(actual.minor).isEqualTo(minor);
    assertThat(actual.patch).isEqualTo(patch);
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "v", "1a", "1.0a", "1.0.0a", "1.0.0.", "1.0.0-" })
  void parseInvalid(String input) {
    testParseInvalid(input);
  }

  private void testParseInvalid(String input) {
    assertThatIllegalStateException().isThrownBy(() -> this.parser.parseVersion(input))
            .withMessage("Invalid API version format");
  }

}