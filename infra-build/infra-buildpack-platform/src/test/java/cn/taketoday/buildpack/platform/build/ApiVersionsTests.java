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

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ApiVersions}.
 *
 * @author Scott Frederick
 */
class ApiVersionsTests {

  @Test
  void findsLatestWhenOneMatchesMajor() {
    ApiVersion version = ApiVersions.parse("1.1", "2.2").findLatestSupported("1.0");
    assertThat(version).isEqualTo(ApiVersion.parse("1.1"));
  }

  @Test
  void findsLatestWhenOneMatchesWithReleaseVersions() {
    ApiVersion version = ApiVersions.parse("1.1", "1.2").findLatestSupported("1.1");
    assertThat(version).isEqualTo(ApiVersion.parse("1.2"));
  }

  @Test
  void findsLatestWhenOneMatchesWithPreReleaseVersions() {
    ApiVersion version = ApiVersions.parse("0.2", "0.3").findLatestSupported("0.2");
    assertThat(version).isEqualTo(ApiVersion.parse("0.2"));
  }

  @Test
  void findsLatestWhenMultipleMatchesWithReleaseVersions() {
    ApiVersion version = ApiVersions.parse("1.1", "1.2").findLatestSupported("1.1", "1.2");
    assertThat(version).isEqualTo(ApiVersion.parse("1.2"));
  }

  @Test
  void findsLatestWhenMultipleMatchesWithPreReleaseVersions() {
    ApiVersion version = ApiVersions.parse("0.2", "0.3").findLatestSupported("0.2", "0.3");
    assertThat(version).isEqualTo(ApiVersion.parse("0.3"));
  }

  @Test
  void findLatestWhenNoneSupportedThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ApiVersions.parse("1.1", "1.2").findLatestSupported("1.3", "1.4"))
            .withMessage("Detected platform API versions '1.3,1.4' are not included in supported versions '1.1,1.2'");
  }

  @Test
  void createFromRange() {
    ApiVersions versions = ApiVersions.of(1, IntStream.rangeClosed(2, 7));
    assertThat(versions).hasToString("1.2,1.3,1.4,1.5,1.6,1.7");
  }

  @Test
  void toStringReturnsString() {
    assertThat(ApiVersions.parse("1.1", "2.2", "3.3")).hasToString("1.1,2.2,3.3");
  }

  @Test
  void equalsAndHashCode() {
    ApiVersions v12a = ApiVersions.parse("1.2", "2.3");
    ApiVersions v12b = ApiVersions.parse("1.2", "2.3");
    ApiVersions v13 = ApiVersions.parse("1.3", "2.4");
    assertThat(v12a).hasSameHashCodeAs(v12b);
    assertThat(v12a).isEqualTo(v12a).isEqualTo(v12b).isNotEqualTo(v13);
  }

}
