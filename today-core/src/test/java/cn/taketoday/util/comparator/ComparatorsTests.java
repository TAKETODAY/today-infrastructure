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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.util.comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Comparators}.
 *
 * @author Mathieu Amblard
 * @author Sam Brannen
 */
class ComparatorsTests {

  @Test
  void nullsLow() {
    assertThat(Comparators.nullsLow().compare("boo", "boo")).isZero();
    assertThat(Comparators.nullsLow().compare(null, null)).isZero();
    assertThat(Comparators.nullsLow().compare(null, "boo")).isNegative();
    assertThat(Comparators.nullsLow().compare("boo", null)).isPositive();
  }

  @Test
  void nullsLowWithExplicitComparator() {
    assertThat(Comparators.nullsLow(String::compareTo).compare("boo", "boo")).isZero();
    assertThat(Comparators.nullsLow(String::compareTo).compare(null, null)).isZero();
    assertThat(Comparators.nullsLow(String::compareTo).compare(null, "boo")).isNegative();
    assertThat(Comparators.nullsLow(String::compareTo).compare("boo", null)).isPositive();
  }

  @Test
  void nullsHigh() {
    assertThat(Comparators.nullsHigh().compare("boo", "boo")).isZero();
    assertThat(Comparators.nullsHigh().compare(null, null)).isZero();
    assertThat(Comparators.nullsHigh().compare(null, "boo")).isPositive();
    assertThat(Comparators.nullsHigh().compare("boo", null)).isNegative();
  }

  @Test
  void nullsHighWithExplicitComparator() {
    assertThat(Comparators.nullsHigh(String::compareTo).compare("boo", "boo")).isZero();
    assertThat(Comparators.nullsHigh(String::compareTo).compare(null, null)).isZero();
    assertThat(Comparators.nullsHigh(String::compareTo).compare(null, "boo")).isPositive();
    assertThat(Comparators.nullsHigh(String::compareTo).compare("boo", null)).isNegative();
  }

}
