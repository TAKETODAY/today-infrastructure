/*
 * Copyright 2017 - 2025 the original author or authors.
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
    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(2);
    assertThat(version.getPatch()).isEqualTo(3);
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
    assertThat(actual.getMajor()).isEqualTo(major);
    assertThat(actual.getMinor()).isEqualTo(minor);
    assertThat(actual.getPatch()).isEqualTo(patch);
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