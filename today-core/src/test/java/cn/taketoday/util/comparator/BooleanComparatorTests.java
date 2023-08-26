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

package cn.taketoday.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BooleanComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
class BooleanComparatorTests {

  @Test
  void shouldCompareWithTrueLow() {
    Comparator<Boolean> c = new BooleanComparator(true);
    assertThat(c.compare(true, false)).isLessThan(0);
    assertThat(c.compare(Boolean.TRUE, Boolean.TRUE)).isEqualTo(0);
  }

  @Test
  void shouldCompareWithTrueHigh() {
    Comparator<Boolean> c = new BooleanComparator(false);
    assertThat(c.compare(true, false)).isGreaterThan(0);
    assertThat(c.compare(Boolean.TRUE, Boolean.TRUE)).isEqualTo(0);
  }

  @Test
  void shouldCompareFromTrueLow() {
    Comparator<Boolean> c = BooleanComparator.TRUE_LOW;
    assertThat(c.compare(true, false)).isLessThan(0);
    assertThat(c.compare(Boolean.TRUE, Boolean.TRUE)).isEqualTo(0);
  }

  @Test
  void shouldCompareFromTrueHigh() {
    Comparator<Boolean> c = BooleanComparator.TRUE_HIGH;
    assertThat(c.compare(true, false)).isGreaterThan(0);
    assertThat(c.compare(Boolean.TRUE, Boolean.TRUE)).isEqualTo(0);
  }

}
