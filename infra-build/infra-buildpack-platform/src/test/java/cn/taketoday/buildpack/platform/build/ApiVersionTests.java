/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ApiVersion}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ApiVersionTests {

  @Test
  void parseWhenVersionIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ApiVersion.parse(null))
            .withMessage("Value must not be empty");
  }

  @Test
  void parseWhenVersionIsEmptyThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ApiVersion.parse(""))
            .withMessage("Value must not be empty");
  }

  @Test
  void parseWhenVersionDoesNotMatchPatternThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ApiVersion.parse("bad"))
            .withMessage("Malformed version number 'bad'");
  }

  @Test
  void parseReturnsVersion() {
    ApiVersion version = ApiVersion.parse("1.2");
    assertThat(version.getMajor()).isOne();
    assertThat(version.getMinor()).isEqualTo(2);
  }

  @Test
  void assertSupportsWhenSupports() {
    ApiVersion.parse("1.2").assertSupports(ApiVersion.parse("1.0"));
  }

  @Test
  void assertSupportsWhenDoesNotSupportThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ApiVersion.parse("1.2").assertSupports(ApiVersion.parse("1.3")))
            .withMessage("Detected platform API version '1.3' does not match supported version '1.2'");
  }

  @Test
  void supportsWhenSame() {
    assertThat(supports("0.0", "0.0")).isTrue();
    assertThat(supports("0.1", "0.1")).isTrue();
    assertThat(supports("1.0", "1.0")).isTrue();
    assertThat(supports("1.1", "1.1")).isTrue();
  }

  @Test
  void supportsWhenDifferentMajor() {
    assertThat(supports("0.0", "1.0")).isFalse();
    assertThat(supports("1.0", "0.0")).isFalse();
    assertThat(supports("1.0", "2.0")).isFalse();
    assertThat(supports("2.0", "1.0")).isFalse();
    assertThat(supports("1.1", "2.1")).isFalse();
    assertThat(supports("2.1", "1.1")).isFalse();
  }

  @Test
  void supportsWhenDifferentMinor() {
    assertThat(supports("1.2", "1.1")).isTrue();
    assertThat(supports("1.2", "1.3")).isFalse();
  }

  @Test
  void supportsWhenMajorZeroAndDifferentMinor() {
    assertThat(supports("0.2", "0.1")).isFalse();
    assertThat(supports("0.2", "0.3")).isFalse();
  }

  @Test
  void supportsAnyWhenOneMatches() {
    assertThat(supportsAny("0.2", "0.1", "0.2")).isTrue();
  }

  @Test
  void supportsAnyWhenNoneMatch() {
    assertThat(supportsAny("0.2", "0.3", "0.4")).isFalse();
  }

  @Test
  void toStringReturnsString() {
    assertThat(ApiVersion.parse("1.2")).hasToString("1.2");
  }

  @Test
  void equalsAndHashCode() {
    ApiVersion v12a = ApiVersion.parse("1.2");
    ApiVersion v12b = ApiVersion.parse("1.2");
    ApiVersion v13 = ApiVersion.parse("1.3");
    assertThat(v12a).hasSameHashCodeAs(v12b);
    assertThat(v12a).isEqualTo(v12a).isEqualTo(v12b).isNotEqualTo(v13);
  }

  private boolean supports(String v1, String v2) {
    return ApiVersion.parse(v1).supports(ApiVersion.parse(v2));
  }

  private boolean supportsAny(String v1, String... others) {
    return ApiVersion.parse(v1)
            .supportsAny(Arrays.stream(others).map(ApiVersion::parse).toArray(ApiVersion[]::new));
  }

}
