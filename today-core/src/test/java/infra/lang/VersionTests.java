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

package infra.lang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/13 16:13
 */
class VersionTests {

  @Test
  void parse() {
    Version.get();

    // 4.0.0-Draft.1  latest  4.0.0-Beta.1 -Alpha.1 -Draft.1 -SNAPSHOT
    Version version = Version.parse("4.0.0-Draft.1");

    assertThat(version.type()).isEqualTo(Version.Draft);
    assertThat(version.step()).isEqualTo(1);
    assertThat(version.major()).isEqualTo(4);
    assertThat(version.minor()).isEqualTo(0);
    assertThat(version.micro()).isEqualTo(0);
    assertThat(version.extension()).isNull();

    // release
    version = Version.parse("4.0.0");
    assertThat(version.type()).isEqualTo(Version.RELEASE);
    assertThat(version.step()).isEqualTo(0);

    // Beta
    version = Version.parse("4.0.0-Beta");
    assertThat(version.type()).isEqualTo(Version.Beta);
    assertThat(version.step()).isEqualTo(0);

    // Beta with step
    version = Version.parse("4.0.0-Beta.3");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Beta);

    // Alpha
    version = Version.parse("4.0.0-Alpha");
    assertThat(version.type()).isEqualTo(Version.Alpha);

    // Alpha with step
    version = Version.parse("4.0.0-Alpha.3");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Alpha);

    // extension
    version = Version.parse("4.0.0-Alpha.3-jdk8");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Alpha);
    assertThat(version.extension()).isEqualTo("jdk8");

    // extension
    version = Version.parse("4.0.0-Alpha.3-SNAPSHOT");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Alpha);
    assertThat(version.extension()).isEqualTo(Version.SNAPSHOT);

  }

  @Test
  void versionParsingShouldHandleAllFormats() {
    assertThat(Version.parse("4.1.2")).satisfies(version -> {
      assertThat(version.major()).isEqualTo(4);
      assertThat(version.minor()).isEqualTo(1);
      assertThat(version.micro()).isEqualTo(2);
      assertThat(version.type()).isEqualTo(Version.RELEASE);
      assertThat(version.step()).isEqualTo(0);
      assertThat(version.extension()).isNull();
    });
  }

  @Test
  void versionComparisonShouldMatchExpectedOrder() {
    Version v1 = Version.parse("4.0.0-Draft.1");
    Version v2 = Version.parse("4.0.0-SNAPSHOT");
    Version v3 = Version.parse("4.0.0-Alpha.1");
    Version v4 = Version.parse("4.0.0-Beta.1");
    Version v5 = Version.parse("4.0.0");

    assertThat(v1).isLessThan(v2);
    assertThat(v2).isLessThan(v3);
    assertThat(v3).isLessThan(v4);
    assertThat(v4).isLessThan(v5);
  }

  @Test
  void equalVersionsShouldBeComparedCorrectly() {
    Version v1 = Version.parse("4.0.0-Beta.1");
    Version v2 = Version.parse("4.0.0-Beta.1");
    Version v3 = Version.parse("4.0.0-Beta.1-jdk8");

    assertThat(v1).isEqualTo(v2);
    assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
    assertThat(v1.compareTo(v2)).isZero();
    assertThat(v1).isNotEqualTo(v3);
  }

  @Test
  void versionsShouldBeComparedByMajorMinorMicro() {
    assertThat(Version.parse("4.0.0")).isGreaterThan(Version.parse("3.9.9"));
    assertThat(Version.parse("4.1.0")).isGreaterThan(Version.parse("4.0.9"));
    assertThat(Version.parse("4.0.1")).isGreaterThan(Version.parse("4.0.0"));
  }

  @Test
  void stepComparisonShouldWorkWithinSameType() {
    assertThat(Version.parse("4.0.0-Beta.2"))
            .isGreaterThan(Version.parse("4.0.0-Beta.1"));
    assertThat(Version.parse("4.0.0-Alpha.3"))
            .isGreaterThan(Version.parse("4.0.0-Alpha.2"));
  }

  @Test
  void toStringShouldIncludeVersionPrefix() {
    Version version = Version.parse("4.0.0-Beta.1");
    assertThat(version.toString()).isEqualTo("v4.0.0-Beta.1");
  }

  @Test
  void shouldHandleInvalidVersionFormats() {
    assertThatThrownBy(() -> Version.parse("invalid"))
            .isInstanceOf(NumberFormatException.class);
    assertThatThrownBy(() -> Version.parse("1.invalid.0"))
            .isInstanceOf(NumberFormatException.class);
    assertThatThrownBy(() -> Version.parse("1.0.0-Beta.invalid"))
            .isInstanceOf(NumberFormatException.class);
  }

  @Test
  void defaultInstanceShouldBeAvailable() {
    Version version = Version.get();
    assertThat(version).isNotNull();
    assertThat(version.implementationVersion()).isNotNull();
  }

  @Test
  void unknownTypesShouldFallbackToStringComparison() {
    Version v1 = Version.parse("4.0.0-RC.1");
    Version v2 = Version.parse("4.0.0-Nightly.1");
    assertThat(v1.compareTo(v2)).isPositive();
  }

  @Test
  void shouldHandleVersionWithoutMinorAndMicro() {
    assertThatThrownBy(() -> Version.parse("4"))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
  }

  @Test
  void shouldParseVersionWithOnlyTypeWithoutStep() {
    Version version = Version.parse("4.1.0-SNAPSHOT");
    assertThat(version.type()).isEqualTo(Version.SNAPSHOT);
    assertThat(version.step()).isZero();
  }

  @Test
  void versionsShouldBeComparedByType() {
    assertThat(Version.parse("3.0")).isGreaterThan(Version.parse("3.0.0-Beta.999"));
    assertThat(Version.parse("3.0-Beta.1")).isGreaterThan(Version.parse("3.0-Alpha.999"));
    assertThat(Version.parse("3.0.0-Alpha.1")).isGreaterThan(Version.parse("3.0.0-SNAPSHOT"));
    assertThat(Version.parse("3.0.0-SNAPSHOT")).isGreaterThan(Version.parse("3.0.0-Draft.999"));
  }

  @Test
  void equalVersionsShouldHaveSameString() {
    Version v1 = Version.parse("4.0.0-Beta.1");
    Version v2 = Version.parse("4.0.0-Beta.1");
    assertThat(v1.toString()).isEqualTo(v2.toString());
    assertThat(v1.toString()).isEqualTo("v4.0.0-Beta.1");
  }

  @Test
  void shouldCompareInstanceToItself() {
    Version version = Version.parse("4.0.0");
    assertThat(version.compareTo(version)).isZero();
  }

  @Test
  void equalsShouldHandleNullAndDifferentTypes() {
    Version version = Version.parse("4.0.0");
    assertThat(version.equals(null)).isFalse();
    assertThat(version.equals("4.0.0")).isFalse();
  }

  @Test
  void equalsShouldCompareImplementationVersionOnly() {
    Version v1 = Version.parse("4.0.0-Alpha.1");
    Version v2 = new Version(4, 0, 0, Version.Alpha, 1, null, "4.0.0-Alpha.1");
    Version v3 = new Version(4, 0, 0, Version.Alpha, 1, null, "different");

    assertThat(v1).isEqualTo(v2);
    assertThat(v1).isNotEqualTo(v3);
  }

  @Test
  void equalsShouldBeReflexiveAndSymmetric() {
    Version v1 = Version.parse("4.0.0");
    Version v2 = Version.parse("4.0.0");

    assertThat(v1.equals(v1)).isTrue();
    assertThat(v1.equals(v2) && v2.equals(v1)).isTrue();
  }

}