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

package infra.util.comparator;

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

  @Test
  void compareWithFalseFirst() {
    BooleanComparator comparator = new BooleanComparator(true);

    assertThat(comparator.compare(false, true)).isGreaterThan(0);
    assertThat(comparator.compare(true, false)).isLessThan(0);
    assertThat(comparator.compare(false, false)).isEqualTo(0);
    assertThat(comparator.compare(true, true)).isEqualTo(0);
  }

  @Test
  void compareWithTrueFirst() {
    BooleanComparator comparator = new BooleanComparator(false);

    assertThat(comparator.compare(false, true)).isLessThan(0);
    assertThat(comparator.compare(true, false)).isGreaterThan(0);
    assertThat(comparator.compare(false, false)).isEqualTo(0);
    assertThat(comparator.compare(true, true)).isEqualTo(0);
  }

  @Test
  void equalsWithSameInstance() {
    BooleanComparator comparator = new BooleanComparator(true);
    assertThat(comparator.equals(comparator)).isTrue();
  }

  @Test
  void equalsWithEqualInstances() {
    BooleanComparator comparator1 = new BooleanComparator(true);
    BooleanComparator comparator2 = new BooleanComparator(true);

    assertThat(comparator1.equals(comparator2)).isTrue();
  }

  @Test
  void equalsWithDifferentFlagValues() {
    BooleanComparator trueLow = new BooleanComparator(true);
    BooleanComparator trueHigh = new BooleanComparator(false);

    assertThat(trueLow.equals(trueHigh)).isFalse();
  }

  @Test
  void equalsWithNull() {
    BooleanComparator comparator = new BooleanComparator(true);
    assertThat(comparator.equals(null)).isFalse();
  }

  @Test
  void equalsWithDifferentObjectType() {
    BooleanComparator comparator = new BooleanComparator(true);
    assertThat(comparator.equals("not a comparator")).isFalse();
  }

  @Test
  void hashCodeWithTrueLow() {
    BooleanComparator comparator = new BooleanComparator(true);
    assertThat(comparator.hashCode()).isEqualTo(Boolean.hashCode(true));
  }

  @Test
  void hashCodeWithTrueHigh() {
    BooleanComparator comparator = new BooleanComparator(false);
    assertThat(comparator.hashCode()).isEqualTo(Boolean.hashCode(false));
  }

  @Test
  void toStringWithTrueLow() {
    BooleanComparator comparator = new BooleanComparator(true);
    assertThat(comparator.toString()).isEqualTo("BooleanComparator: true low");
  }

  @Test
  void toStringWithTrueHigh() {
    BooleanComparator comparator = new BooleanComparator(false);
    assertThat(comparator.toString()).isEqualTo("BooleanComparator: true high");
  }

  @Test
  void sharedInstancesAreSingletons() {
    assertThat(BooleanComparator.TRUE_LOW).isSameAs(BooleanComparator.TRUE_LOW);
    assertThat(BooleanComparator.TRUE_HIGH).isSameAs(BooleanComparator.TRUE_HIGH);
    assertThat(BooleanComparator.TRUE_LOW).isNotSameAs(BooleanComparator.TRUE_HIGH);
  }

  @Test
  void sharedInstancesBehaveCorrectly() {
    assertThat(BooleanComparator.TRUE_LOW.compare(true, false)).isLessThan(0);
    assertThat(BooleanComparator.TRUE_HIGH.compare(true, false)).isGreaterThan(0);
  }

}
