/*
 * Copyright 2017 - 2026 the TODAY authors.
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

  @Test
  void typeShouldBeDetectedCorrectly() {
    assertThat(Version.parse("4.0.0-Draft.1").isDraft()).isTrue();
    assertThat(Version.parse("4.0.0-SNAPSHOT").isSnapshot()).isTrue();
    assertThat(Version.parse("4.0.0-Alpha.1").isAlpha()).isTrue();
    assertThat(Version.parse("4.0.0-Beta.1").isBeta()).isTrue();
    assertThat(Version.parse("4.0.0").isRelease()).isTrue();

    assertThat(Version.parse("4.0.0-Alpha.1").isPreRelease()).isTrue();
    assertThat(Version.parse("4.0.0").isPreRelease()).isFalse();
  }

  @Test
  void convenientComparisonMethodsShouldWork() {
    Version release = Version.parse("4.1.0");
    Version beta = Version.parse("4.1.0-Beta.1");
    Version alpha = Version.parse("4.1.0-Alpha.1");

    assertThat(release.isNewerThan(beta)).isTrue();
    assertThat(beta.isNewerThan(alpha)).isTrue();
    assertThat(alpha.isOlderThan(beta)).isTrue();
    assertThat(beta.isOlderThan(release)).isTrue();

    assertThat(release.isEqualOrNewerThan(beta)).isTrue();
    assertThat(release.isEqualOrNewerThan(release)).isTrue();
    assertThat(alpha.isEqualOrOlderThan(beta)).isTrue();
    assertThat(alpha.isEqualOrOlderThan(alpha)).isTrue();
    assertThat(beta.isEqualOrNewerThan(release)).isFalse();
    assertThat(release.isEqualOrOlderThan(alpha)).isFalse();
  }

  @Test
  void matchesShouldCheckMajorMinorMicroOnly() {
    Version version = Version.parse("4.1.2-Beta.3");

    assertThat(version.matches(4, 1, 2)).isTrue();
    assertThat(version.matches(4, 1, 3)).isFalse();
    assertThat(version.matches(3, 1, 2)).isFalse();
  }

  @Test
  void toVersionStringShouldDropVPrefix() {
    Version version = Version.parse("4.0.0-Beta.1");
    assertThat(version.toVersionString()).isEqualTo("4.0.0-Beta.1");
    assertThat(version.toString()).isEqualTo("v" + version.toVersionString());
  }

  @Test
  void withoutExtensionShouldReturnVersionWithoutSuffix() {
    Version withExtension = Version.parse("4.0.0-Alpha.3-jdk8");
    Version without = withExtension.withoutExtension();

    assertThat(without.extension()).isNull();
    assertThat(without.type()).isEqualTo(Version.Alpha);
    assertThat(without.step()).isEqualTo(3);
    assertThat(without.implementationVersion()).isEqualTo("4.0.0-Alpha.3");
  }

  @Test
  void withoutExtensionShouldReturnSameInstanceWhenNoExtension() {
    Version version = Version.parse("4.0.0-Alpha.3");
    assertThat(version.withoutExtension()).isSameAs(version);
  }

  @Test
  void parseShouldBePubliclyAccessible() {
    Version version = Version.parse("3.2.1-Beta.2");
    assertThat(version.major()).isEqualTo(3);
    assertThat(version.minor()).isEqualTo(2);
    assertThat(version.micro()).isEqualTo(1);
    assertThat(version.type()).isEqualTo(Version.Beta);
    assertThat(version.step()).isEqualTo(2);
  }

  @Test
  void withExtensionShouldAddExtensionToExtensionlessVersion() {
    Version version = Version.parse("4.0.0-Alpha.3");
    Version withExtension = version.withExtension("jdk8");

    assertThat(withExtension.extension()).isEqualTo("jdk8");
    assertThat(withExtension.type()).isEqualTo(Version.Alpha);
    assertThat(withExtension.step()).isEqualTo(3);
    assertThat(withExtension.implementationVersion()).isEqualTo("4.0.0-Alpha.3-jdk8");
  }

  @Test
  void withExtensionShouldReplaceExistingExtension() {
    Version version = Version.parse("4.0.0-Alpha.3-jdk8");
    Version withExtension = version.withExtension("jdk17");

    assertThat(withExtension.extension()).isEqualTo("jdk17");
    assertThat(withExtension.major()).isEqualTo(4);
    assertThat(withExtension.minor()).isEqualTo(0);
    assertThat(withExtension.micro()).isEqualTo(0);
    assertThat(withExtension.type()).isEqualTo(Version.Alpha);
    assertThat(withExtension.step()).isEqualTo(3);
    assertThat(withExtension.implementationVersion()).isEqualTo("4.0.0-Alpha.3-jdk17");
  }

  @Test
  void withExtensionShouldRemoveExtensionWhenNull() {
    Version version = Version.parse("4.0.0-Alpha.3-jdk8");
    Version without = version.withExtension(null);

    assertThat(without.extension()).isNull();
    assertThat(without.implementationVersion()).isEqualTo("4.0.0-Alpha.3");
  }

  @Test
  void withExtensionShouldReturnSameInstanceWhenUnchanged() {
    Version version = Version.parse("4.0.0-Alpha.3-jdk8");
    assertThat(version.withExtension("jdk8")).isSameAs(version);

    Version extensionless = Version.parse("4.0.0-Alpha.3");
    assertThat(extensionless.withExtension(null)).isSameAs(extensionless);
  }

  @Test
  void parseShouldKeepHyphenatedExtension() {
    Version version = Version.parse("4.0.0-Alpha.3-my-jdk");

    assertThat(version.type()).isEqualTo(Version.Alpha);
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.extension()).isEqualTo("my-jdk");
    assertThat(version.implementationVersion()).isEqualTo("4.0.0-Alpha.3-my-jdk");
  }

  @Test
  void withExtensionAndParseShouldBeSymmetricForHyphenatedExtension() {
    Version original = Version.parse("4.0.0-Alpha.3");
    Version withExtension = original.withExtension("my-jdk");

    assertThat(withExtension.implementationVersion()).isEqualTo("4.0.0-Alpha.3-my-jdk");
    assertThat(withExtension.extension()).isEqualTo("my-jdk");

    Version reparsed = Version.parse(withExtension.implementationVersion());
    assertThat(reparsed.extension()).isEqualTo("my-jdk");
    assertThat(reparsed.type()).isEqualTo(Version.Alpha);
    assertThat(reparsed.step()).isEqualTo(3);
    assertThat(reparsed).isEqualTo(withExtension);
  }

}