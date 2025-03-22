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

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/14 18:49
 */
class StreamIterableTests {

  @Test
  void test() {
    StreamIterable<String> objects = new StreamIterable<>(Stream.of("1"));
    assertThat(objects).hasSize(1);
    assertThat(new StreamIterable<>(Stream.of("1"))).contains("1");
  }

  @Test
  void emptyStreamCreatesEmptyIterable() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.empty());
    assertThat(iterable).isEmpty();
  }

  @Test
  void iterableSupportsMultipleValues() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a", "b", "c"));
    assertThat(iterable).containsExactly("a", "b", "c");
  }

  @Test
  void forEachIteratesOverAllElements() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a", "b", "c"));
    StringBuilder result = new StringBuilder();
    iterable.forEach(result::append);
    assertThat(result.toString()).isEqualTo("abc");
  }

  @Test
  void iterableCanContainNullValues() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a", null, "c"));
    assertThat(iterable).containsExactly("a", null, "c");
  }

  @Test
  void streamCanBeConsumedOnlyOnce() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a", "b"));
    iterable.forEach(s -> { });
    assertThatThrownBy(() -> iterable.forEach(s -> { }))
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void iteratorFollowsStreamOrder() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a", "b", "c"));
    Iterator<String> iterator = iterable.iterator();
    assertThat(iterator.next()).isEqualTo("a");
    assertThat(iterator.next()).isEqualTo("b");
    assertThat(iterator.next()).isEqualTo("c");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void parallelStreamRetainsCharacteristics() {
    Stream<String> parallelStream = Stream.of("a", "b", "c").parallel();
    StreamIterable<String> iterable = new StreamIterable<>(parallelStream);
    Spliterator<String> spliterator = iterable.spliterator();
    assertThat(spliterator.hasCharacteristics(Spliterator.CONCURRENT | Spliterator.IMMUTABLE)).isFalse();
  }

  @Test
  void forEachRemainingHandlesAllElements() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a", "b", "c"));
    Iterator<String> iterator = iterable.iterator();
    StringBuilder result = new StringBuilder();
    iterator.forEachRemaining(result::append);
    assertThat(result.toString()).isEqualTo("abc");
  }

  @Test
  void iteratorRemoveOperationThrowsException() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a"));
    Iterator<String> iterator = iterable.iterator();
    iterator.next();
    assertThatThrownBy(iterator::remove)
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void infiniteStreamCanBeIterated() {
    Stream<Integer> infiniteStream = Stream.iterate(1, i -> i + 1).limit(1000);
    StreamIterable<Integer> iterable = new StreamIterable<>(infiniteStream);
    Iterator<Integer> iterator = iterable.iterator();
    assertThat(iterator.next()).isEqualTo(1);
    assertThat(iterator.next()).isEqualTo(2);
  }

  @Test
  void streamWithCustomTypeIsSupported() {
    record CustomType(String value) { }
    CustomType item = new CustomType("test");
    StreamIterable<CustomType> iterable = new StreamIterable<>(Stream.of(item));
    assertThat(iterable).containsExactly(item);
  }

  @Test
  void streamOperationsPreserveOrder() {
    StreamIterable<String> iterable = new StreamIterable<>(
            Stream.of("c", "a", "b").sorted());
    assertThat(iterable).containsExactly("a", "b", "c");
  }

  @Test
  void iteratorNextWithoutHasNextWorks() {
    StreamIterable<String> iterable = new StreamIterable<>(Stream.of("a"));
    Iterator<String> iterator = iterable.iterator();
    assertThat(iterator.next()).isEqualTo("a");
  }

}