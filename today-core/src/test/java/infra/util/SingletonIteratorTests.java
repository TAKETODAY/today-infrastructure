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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 20:00
 */
class SingletonIteratorTests {

  @Test
  void iteratorReturnsTheElementExactlyOnce() {
    Iterator<String> iterator = CollectionUtils.singletonIterator("test");
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("test");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void nextThrowsExceptionWhenCalledMoreThanOnce() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    iterator.next();
    assertThatThrownBy(iterator::next)
            .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void forEachRemainingExecutesActionExactlyOnce() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    List<String> items = new ArrayList<>();
    iterator.forEachRemaining(items::add);

    assertThat(items).containsExactly("test");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void nullElementIsAllowed() {
    SingletonIterator<String> iterator = new SingletonIterator<>(null);
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isNull();
  }

  @Test
  void removeOperationIsNotSupported() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    assertThatThrownBy(iterator::remove)
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void forEachRemainingPreventsFurtherIteration() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    List<String> items = new ArrayList<>();

    iterator.forEachRemaining(items::add);
    assertThatThrownBy(iterator::next)
            .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void forEachRemainingWithNullActionThrowsException() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    assertThatThrownBy(() -> iterator.forEachRemaining(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void multipleHasNextCallsReturnSameResult() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.hasNext()).isTrue();

    iterator.next();

    assertThat(iterator.hasNext()).isFalse();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void hasNextDoesNotChangeIteratorState() {
    SingletonIterator<String> iterator = new SingletonIterator<>("test");
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("test");
  }

}