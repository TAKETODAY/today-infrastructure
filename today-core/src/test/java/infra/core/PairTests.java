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

package infra.core;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 18:21
 */
class PairTests {

  @Test
  void createEmptyPair() {
    var pair = Pair.empty();
    assertThat(pair.getFirst()).isNull();
    assertThat(pair.getSecond()).isNull();
  }

  @Test
  void createPairWithValues() {
    var pair = Pair.of("first", 42);
    assertThat(pair.getFirst()).isEqualTo("first");
    assertThat(pair.getSecond()).isEqualTo(42);
  }

  @Test
  void getKeyReturnsFirstValue() {
    var pair = Pair.of("key", "value");
    assertThat(pair.getKey()).isEqualTo("key");
  }

  @Test
  void getValueReturnsSecondValue() {
    var pair = Pair.of("key", "value");
    assertThat(pair.getValue()).isEqualTo("value");
  }

  @Test
  void setValueThrowsUnsupportedException() {
    var pair = Pair.of("key", "value");
    assertThatThrownBy(() -> pair.setValue("newValue"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void withFirstCreatesNewPairWithUpdatedFirst() {
    var pair = Pair.of("old", 42);
    var newPair = pair.withFirst("new");

    assertThat(newPair.getFirst()).isEqualTo("new");
    assertThat(newPair.getSecond()).isEqualTo(42);
    assertThat(newPair).isNotSameAs(pair);
  }

  @Test
  void withFirstReturnsSameInstanceWhenValueUnchanged() {
    var pair = Pair.of("same", 42);
    var newPair = pair.withFirst("same");
    assertThat(newPair).isSameAs(pair);
  }

  @Test
  void withSecondCreatesNewPairWithUpdatedSecond() {
    var pair = Pair.of("key", 42);
    var newPair = pair.withSecond(99);

    assertThat(newPair.getFirst()).isEqualTo("key");
    assertThat(newPair.getSecond()).isEqualTo(99);
    assertThat(newPair).isNotSameAs(pair);
  }

  @Test
  void withSecondReturnsSameInstanceWhenValueUnchanged() {
    var pair = Pair.of("key", 42);
    var newPair = pair.withSecond(42);
    assertThat(newPair).isSameAs(pair);
  }

  @Test
  void firstReturnsOptionalOfFirstValue() {
    assertThat(Pair.of("value", 42).first()).contains("value");
    assertThat(Pair.of(null, 42).first()).isEmpty();
  }

  @Test
  void secondReturnsOptionalOfSecondValue() {
    assertThat(Pair.of("key", 42).second()).contains(42);
    assertThat(Pair.of("key", null).second()).isEmpty();
  }

  @Test
  void equalsComparesFirstAndSecondValues() {
    var pair1 = Pair.of("key", 42);
    var pair2 = Pair.of("key", 42);
    var differentPair = Pair.of("different", 99);

    assertThat(pair1).isEqualTo(pair2);
    assertThat(pair1).isNotEqualTo(differentPair);
    assertThat(pair1).isNotEqualTo(null);
    assertThat(pair1).isNotEqualTo("not a pair");
  }

  @Test
  void hashCodeIncludesFirstAndSecondValues() {
    var pair1 = Pair.of("key", 42);
    var pair2 = Pair.of("key", 42);

    assertThat(pair1.hashCode()).isEqualTo(pair2.hashCode());
  }

  @Test
  void toStringContainsFirstAndSecondValues() {
    var pair = Pair.of("key", 42);
    assertThat(pair.toString()).isEqualTo("<key,42>");
  }

  @Test
  void getFirstReturnsNullForNullPair() {
    Object first = Pair.first(null);
    assertThat(first).isNull();
  }

  @Test
  void getSecondReturnsNullForNullPair() {
    Object second = Pair.second(null);
    assertThat(second).isNull();
  }

  @Test
  void comparingFirstCreatesComparatorUsingFirstValue() {
    var pairs = List.of(
            Pair.of("c", 1),
            Pair.of("a", 2),
            Pair.of("b", 3)
    );

    var sorted = pairs.stream()
            .sorted(Pair.comparingFirst())
            .toList();

    assertThat(sorted).extracting(Pair::getFirst)
            .containsExactly("a", "b", "c");
  }

  @Test
  void comparingSecondCreatesComparatorUsingSecondValue() {
    var pairs = List.of(
            Pair.of("a", 3),
            Pair.of("b", 1),
            Pair.of("c", 2)
    );

    var sorted = pairs.stream()
            .sorted(Pair.comparingSecond())
            .toList();

    assertThat(sorted).extracting(Pair::getSecond)
            .containsExactly(1, 2, 3);
  }

  @Test
  void emptyPairSingletonInstanceIsReused() {
    var empty1 = Pair.empty();
    var empty2 = Pair.empty();
    assertThat(empty1).isSameAs(empty2);
  }

  @Test
  void equalsHandlesNullValues() {
    var pair1 = Pair.of(null, null);
    var pair2 = Pair.of(null, null);
    var pair3 = Pair.of("a", null);
    var pair4 = Pair.of(null, "b");

    assertThat(pair1).isEqualTo(pair2);
    assertThat(pair1).isNotEqualTo(pair3);
    assertThat(pair1).isNotEqualTo(pair4);
  }

  @Test
  void hashCodeHandlesNullValues() {
    var pair1 = Pair.of(null, null);
    var pair2 = Pair.of(null, null);

    assertThat(pair1.hashCode()).isEqualTo(pair2.hashCode());
  }

  @Test
  void toStringHandlesNullValues() {
    var pair = Pair.of(null, null);
    assertThat(pair.toString()).isEqualTo("<null,null>");
  }

  @Test
  void withFirstHandlesNullValues() {
    var pair = Pair.<String, @Nullable String>of("a", "b");
    var newPair = pair.withFirst(null);
    assertThat(newPair.getFirst()).isNull();
    assertThat(newPair.getSecond()).isEqualTo("b");
  }

  @Test
  void withSecondHandlesNullValues() {
    var pair = Pair.<String, @Nullable String>of("a", "b");
    var newPair = pair.withSecond(null);
    assertThat(newPair.getFirst()).isEqualTo("a");
    assertThat(newPair.getSecond()).isNull();
  }

}