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