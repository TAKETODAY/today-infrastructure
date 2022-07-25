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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ComparableComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
class ComparableComparatorTests {

  @Test
  void comparableComparator() {
    Comparator<String> c = new ComparableComparator<>();
    String s1 = "abc";
    String s2 = "cde";
    assertThat(c.compare(s1, s2) < 0).isTrue();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  void shouldNeedComparable() {
    Comparator c = new ComparableComparator();
    Object o1 = new Object();
    Object o2 = new Object();
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
            c.compare(o1, o2));
  }

}
