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

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/22 17:24
 */
class ArrayIteratorTests {

  @Test
  void iterator() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> arrayIterator = new ArrayIterator<>(strings);

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("1");

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("2");

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("3");

    assertThat(arrayIterator.hasNext()).isFalse();

    assertThatThrownBy(arrayIterator::next).isInstanceOf(NoSuchElementException.class);

  }

  @Test
  void remove() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> arrayIterator = new ArrayIterator<>(strings);
    assertThatThrownBy(arrayIterator::remove).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void offset() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> arrayIterator = new ArrayIterator<>(strings, 1, 2);

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("2");

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("3");

    assertThat(arrayIterator.hasNext()).isFalse();
  }

  @Test
  void emptyArrayShouldHaveNoElements() {
    String[] empty = {};
    ArrayIterator<String> iterator = new ArrayIterator<>(empty);
    assertThat(iterator.hasNext()).isFalse();
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void enumerationMethodsShouldBehaveLikeIterator() {
    String[] strings = { "1", "2" };
    ArrayIterator<String> iterator = new ArrayIterator<>(strings);

    assertThat(iterator.hasMoreElements()).isTrue();
    assertThat(iterator.nextElement()).isEqualTo("1");
    assertThat(iterator.hasMoreElements()).isTrue();
    assertThat(iterator.nextElement()).isEqualTo("2");
    assertThat(iterator.hasMoreElements()).isFalse();
  }

  @Test
  void offsetAndLengthShouldRespectBounds() {
    String[] strings = { "1", "2", "3", "4", "5" };
    ArrayIterator<String> iterator = new ArrayIterator<>(strings, 1, 3);

    assertThat(iterator.next()).isEqualTo("2");
    assertThat(iterator.next()).isEqualTo("3");
    assertThat(iterator.next()).isEqualTo("4");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void asIteratorShouldReturnSameInstance() {
    String[] strings = { "1" };
    ArrayIterator<String> iterator = new ArrayIterator<>(strings);
    assertThat(iterator.asIterator()).isSameAs(iterator);
  }

  @Test
  void nullElementsShouldBeAllowed() {
    String[] withNull = { "1", null, "3" };
    ArrayIterator<String> iterator = new ArrayIterator<>(withNull);

    assertThat(iterator.next()).isEqualTo("1");
    assertThat(iterator.next()).isNull();
    assertThat(iterator.next()).isEqualTo("3");
  }

  @Test
  void zeroLengthWithOffsetShouldHaveNoElements() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> iterator = new ArrayIterator<>(strings, 1, 0);

    assertThat(iterator.hasNext()).isFalse();
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
  }

}