/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util.comparator;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

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

  @Test
  void constructorWithNonNullComparatorAndNullsLow() {
    Comparator<String> delegate = String.CASE_INSENSITIVE_ORDER;
    NullSafeComparator<String> comparator = new NullSafeComparator<>(delegate, true);

    assertThat(comparator.compare(null, null)).isZero();
    assertThat(comparator.compare(null, "hello")).isNegative();
    assertThat(comparator.compare("hello", null)).isPositive();
    assertThat(comparator.compare("HELLO", "hello")).isZero();
  }

  @Test
  void constructorWithNonNullComparatorAndNullsHigh() {
    Comparator<String> delegate = String.CASE_INSENSITIVE_ORDER;
    NullSafeComparator<String> comparator = new NullSafeComparator<>(delegate, false);

    assertThat(comparator.compare(null, null)).isZero();
    assertThat(comparator.compare(null, "hello")).isPositive();
    assertThat(comparator.compare("hello", null)).isNegative();
    assertThat(comparator.compare("HELLO", "hello")).isZero();
  }

  @Test
  void compareWithNonNullValuesUsingNaturalOrder() {
    NullSafeComparator<String> comparator = new NullSafeComparator<>(String::compareTo, true);

    assertThat(comparator.compare("a", "b")).isNegative();
    assertThat(comparator.compare("b", "a")).isPositive();
    assertThat(comparator.compare("a", "a")).isZero();
  }

  @Test
  void compareWithNonNullValuesUsingCustomComparator() {
    Comparator<Integer> reverseComparator = (o1, o2) -> o2.compareTo(o1);
    NullSafeComparator<Integer> comparator = new NullSafeComparator<>(reverseComparator, true);

    assertThat(comparator.compare(1, 2)).isPositive();
    assertThat(comparator.compare(2, 1)).isNegative();
    assertThat(comparator.compare(1, 1)).isZero();
  }

  @Test
  void equalsWithSameInstance() {
    NullSafeComparator<String> comparator = new NullSafeComparator<>(String::compareTo, true);
    assertThat(comparator.equals(comparator)).isTrue();
  }

  @Test
  void equalsWithEqualInstances() {
    Comparator<String> delegate = String::compareTo;
    NullSafeComparator<String> comparator1 = new NullSafeComparator<>(delegate, true);
    NullSafeComparator<String> comparator2 = new NullSafeComparator<>(delegate, true);

    assertThat(comparator1.equals(comparator2)).isTrue();
  }

  @Test
  void equalsWithDifferentNullsLowFlag() {
    Comparator<String> delegate = String::compareTo;
    NullSafeComparator<String> comparator1 = new NullSafeComparator<>(delegate, true);
    NullSafeComparator<String> comparator2 = new NullSafeComparator<>(delegate, false);

    assertThat(comparator1.equals(comparator2)).isFalse();
  }

  @Test
  void equalsWithDifferentComparator() {
    NullSafeComparator<String> comparator1 = new NullSafeComparator<>(String::compareTo, true);
    NullSafeComparator<String> comparator2 = new NullSafeComparator<>(String.CASE_INSENSITIVE_ORDER, true);

    assertThat(comparator1.equals(comparator2)).isFalse();
  }

  @Test
  void equalsWithNull() {
    NullSafeComparator<String> comparator = new NullSafeComparator<>(String::compareTo, true);
    assertThat(comparator.equals(null)).isFalse();
  }

  @Test
  void equalsWithDifferentObjectType() {
    NullSafeComparator<String> comparator = new NullSafeComparator<>(String::compareTo, true);
    assertThat(comparator.equals("not a comparator")).isFalse();
  }

  @Test
  void hashCodeWithNullsLow() {
    NullSafeComparator<String> comparator = new NullSafeComparator<>(String::compareTo, true);
    assertThat(comparator.hashCode()).isEqualTo(Boolean.hashCode(true));
  }

  @Test
  void hashCodeWithNullsHigh() {
    NullSafeComparator<String> comparator = new NullSafeComparator<>(String::compareTo, false);
    assertThat(comparator.hashCode()).isEqualTo(Boolean.hashCode(false));
  }

  @Test
  void toStringWithNullsLow() {
    Comparator<String> delegate = String::compareTo;
    NullSafeComparator<String> comparator = new NullSafeComparator<>(delegate, true);
    assertThat(comparator.toString()).contains("nulls low");
  }

  @Test
  void toStringWithNullsHigh() {
    Comparator<String> delegate = String::compareTo;
    NullSafeComparator<String> comparator = new NullSafeComparator<>(delegate, false);
    assertThat(comparator.toString()).contains("nulls high");
  }

  @Test
  void compareWithMixedTypesUsingComparable() {
    NullSafeComparator<Integer> comparator = new NullSafeComparator<>(Integer::compareTo, true);

    assertThat(comparator.compare(null, 1)).isNegative();
    assertThat(comparator.compare(1, null)).isPositive();
    assertThat(comparator.compare(null, null)).isZero();
    assertThat(comparator.compare(1, 2)).isNegative();
    assertThat(comparator.compare(2, 1)).isPositive();
  }

  @Test
  void compareWithComplexObjects() {
    Comparator<String> lengthComparator = Comparator.comparing(String::length);
    NullSafeComparator<String> comparator = new NullSafeComparator<>(lengthComparator, true);

    assertThat(comparator.compare(null, "hi")).isNegative();
    assertThat(comparator.compare("hi", null)).isPositive();
    assertThat(comparator.compare("hi", "hello")).isNegative();
    assertThat(comparator.compare("hello", "hi")).isPositive();
    assertThat(comparator.compare("hi", "go")).isZero();
  }

}
