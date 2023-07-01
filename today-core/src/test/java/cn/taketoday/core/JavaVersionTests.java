/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 22:50
 */
class JavaVersionTests {

  @Test
  void getJavaVersionShouldBeAvailable() {
    assertThat(JavaVersion.getJavaVersion()).isNotNull();
  }

  @Test
  void compareToWhenComparingSmallerToGreaterShouldBeLessThanZero() {
    assertThat(JavaVersion.SEVENTEEN).isLessThan(JavaVersion.EIGHTEEN);
  }

  @Test
  void compareToWhenComparingGreaterToSmallerShouldBeGreaterThanZero() {
    assertThat(JavaVersion.EIGHTEEN).isGreaterThan(JavaVersion.SEVENTEEN);
  }

  @Test
  void compareToWhenComparingSameShouldBeZero() {
    assertThat(JavaVersion.SEVENTEEN).isEqualByComparingTo(JavaVersion.SEVENTEEN);
  }

  @Test
  void isEqualOrNewerThanWhenComparingSameShouldBeTrue() {
    assertThat(JavaVersion.SEVENTEEN.isEqualOrNewerThan(JavaVersion.SEVENTEEN)).isTrue();
  }

  @Test
  void isEqualOrNewerThanWhenSmallerToGreaterShouldBeFalse() {
    assertThat(JavaVersion.SEVENTEEN.isEqualOrNewerThan(JavaVersion.EIGHTEEN)).isFalse();
  }

  @Test
  void isEqualOrNewerThanWhenGreaterToSmallerShouldBeTrue() {
    assertThat(JavaVersion.EIGHTEEN.isEqualOrNewerThan(JavaVersion.SEVENTEEN)).isTrue();
  }

  @Test
  void isOlderThanThanWhenComparingSameShouldBeFalse() {
    assertThat(JavaVersion.SEVENTEEN.isOlderThan(JavaVersion.SEVENTEEN)).isFalse();
  }

  @Test
  void isOlderThanWhenSmallerToGreaterShouldBeTrue() {
    assertThat(JavaVersion.SEVENTEEN.isOlderThan(JavaVersion.EIGHTEEN)).isTrue();
  }

  @Test
  void isOlderThanWhenGreaterToSmallerShouldBeFalse() {
    assertThat(JavaVersion.EIGHTEEN.isOlderThan(JavaVersion.SEVENTEEN)).isFalse();
  }

  @Test
  @EnabledOnJre(JRE.JAVA_17)
  void currentJavaVersionSeventeen() {
    assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.SEVENTEEN);
  }

  @Test
  @EnabledOnJre(JRE.JAVA_18)
  void currentJavaVersionEighteen() {
    assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.EIGHTEEN);
  }

  @Test
  @EnabledOnJre(JRE.JAVA_19)
  void currentJavaVersionNineteen() {
    assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.NINETEEN);
  }

  @Test
  @EnabledOnJre(JRE.JAVA_20)
  void currentJavaVersionTwenty() {
    assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.TWENTY);
  }

  @Test
  @EnabledOnJre(JRE.JAVA_21)
  void currentJavaVersionTwentyOne() {
    assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.TWENTY_ONE);
  }

}