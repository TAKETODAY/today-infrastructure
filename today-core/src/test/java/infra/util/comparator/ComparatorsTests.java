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

  @Test
  void comparableWithStrings() {
    Comparator<String> comparator = Comparators.comparable();
    assertThat(comparator.compare("apple", "banana")).isNegative();
    assertThat(comparator.compare("banana", "apple")).isPositive();
    assertThat(comparator.compare("apple", "apple")).isZero();
  }

  @Test
  void comparableWithIntegers() {
    Comparator<Integer> comparator = Comparators.comparable();
    assertThat(comparator.compare(1, 2)).isNegative();
    assertThat(comparator.compare(2, 1)).isPositive();
    assertThat(comparator.compare(1, 1)).isZero();
  }

  @Test
  void nullsLowWithCustomComparator() {
    Comparator<String> lengthComparator = Comparator.comparing(String::length);
    Comparator<String> comparator = Comparators.nullsLow(lengthComparator);

    assertThat(comparator.compare(null, "hi")).isNegative();
    assertThat(comparator.compare("hi", null)).isPositive();
    assertThat(comparator.compare(null, null)).isZero();
    assertThat(comparator.compare("hi", "go")).isZero();
    assertThat(comparator.compare("hi", "hello")).isNegative();
  }

  @Test
  void nullsHighWithCustomComparator() {
    Comparator<String> lengthComparator = Comparator.comparing(String::length);
    Comparator<String> comparator = Comparators.nullsHigh(lengthComparator);

    assertThat(comparator.compare(null, "hi")).isPositive();
    assertThat(comparator.compare("hi", null)).isNegative();
    assertThat(comparator.compare(null, null)).isZero();
    assertThat(comparator.compare("hi", "go")).isZero();
    assertThat(comparator.compare("hi", "hello")).isNegative();
  }

  @Test
  void nullsLowAndNullsHighReturnDifferentOrderings() {
    Comparator<String> nullsLow = Comparators.nullsLow();
    Comparator<String> nullsHigh = Comparators.nullsHigh();

    assertThat(nullsLow.compare(null, "test")).isNegative();
    assertThat(nullsHigh.compare(null, "test")).isPositive();

    assertThat(nullsLow.compare("test", null)).isPositive();
    assertThat(nullsHigh.compare("test", null)).isNegative();
  }

  @Test
  void comparableReturnsSameInstance() {
    Comparator<String> comparator1 = Comparators.comparable();
    Comparator<Integer> comparator2 = Comparators.comparable();

    // These should be functionally equivalent
    assertThat(comparator1.compare("a", "b")).isNegative();
    assertThat(comparator2.compare(1, 2)).isNegative();
  }

}
