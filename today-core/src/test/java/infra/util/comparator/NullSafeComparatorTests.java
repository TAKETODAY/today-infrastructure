/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import infra.util.comparator.NullSafeComparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NullSafeComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
class NullSafeComparatorTests {

  @Test
  @SuppressWarnings("unchecked")
  void shouldCompareWithNullsLow() {
    Comparator<String> c = NullSafeComparator.NULLS_LOW;

    assertThat(c.compare("boo", "boo")).isZero();
    assertThat(c.compare(null, null)).isZero();
    assertThat(c.compare(null, "boo")).isNegative();
    assertThat(c.compare("boo", null)).isPositive();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCompareWithNullsHigh() {
    Comparator<String> c = NullSafeComparator.NULLS_HIGH;

    assertThat(c.compare("boo", "boo")).isZero();
    assertThat(c.compare(null, null)).isZero();
    assertThat(c.compare(null, "boo")).isPositive();
    assertThat(c.compare("boo", null)).isNegative();
  }

}
