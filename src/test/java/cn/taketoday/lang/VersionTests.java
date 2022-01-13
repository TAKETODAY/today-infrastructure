package cn.taketoday.lang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

  }
}