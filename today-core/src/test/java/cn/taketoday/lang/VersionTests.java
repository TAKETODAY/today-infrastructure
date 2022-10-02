/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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