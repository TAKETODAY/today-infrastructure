/*
 * Copyright 2017 - 2026 the TODAY authors.
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