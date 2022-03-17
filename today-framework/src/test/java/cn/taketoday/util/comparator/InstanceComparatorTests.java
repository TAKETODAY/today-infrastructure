/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstanceComparator}.
 *
 * @author Phillip Webb
 */
class InstanceComparatorTests {

  private C1 c1 = new C1();

  private C2 c2 = new C2();

  private C3 c3 = new C3();

  private C4 c4 = new C4();

  @Test
  void shouldCompareClasses() throws Exception {
    Comparator<Object> comparator = new InstanceComparator<>(C1.class, C2.class);
    assertThat(comparator.compare(c1, c1)).isEqualTo(0);
    assertThat(comparator.compare(c1, c2)).isEqualTo(-1);
    assertThat(comparator.compare(c2, c1)).isEqualTo(1);
    assertThat(comparator.compare(c2, c3)).isEqualTo(-1);
    assertThat(comparator.compare(c2, c4)).isEqualTo(-1);
    assertThat(comparator.compare(c3, c4)).isEqualTo(0);
  }

  @Test
  void shouldCompareInterfaces() throws Exception {
    Comparator<Object> comparator = new InstanceComparator<>(I1.class, I2.class);
    assertThat(comparator.compare(c1, c1)).isEqualTo(0);
    assertThat(comparator.compare(c1, c2)).isEqualTo(0);
    assertThat(comparator.compare(c2, c1)).isEqualTo(0);
    assertThat(comparator.compare(c1, c3)).isEqualTo(-1);
    assertThat(comparator.compare(c3, c1)).isEqualTo(1);
    assertThat(comparator.compare(c3, c4)).isEqualTo(0);
  }

  @Test
  void shouldCompareMix() throws Exception {
    Comparator<Object> comparator = new InstanceComparator<>(I1.class, C3.class);
    assertThat(comparator.compare(c1, c1)).isEqualTo(0);
    assertThat(comparator.compare(c3, c4)).isEqualTo(-1);
    assertThat(comparator.compare(c3, null)).isEqualTo(-1);
    assertThat(comparator.compare(c4, null)).isEqualTo(0);
  }

  private static interface I1 {

  }

  private static interface I2 {

  }

  private static class C1 implements I1 {
  }

  private static class C2 implements I1 {
  }

  private static class C3 implements I2 {
  }

  private static class C4 implements I2 {
  }

}
