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