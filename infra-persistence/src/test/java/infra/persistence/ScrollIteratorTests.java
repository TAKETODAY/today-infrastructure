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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ScrollIteratorTests {

  private static final List<Integer> VALUES = List.of(0, 1, 2, 3, 4);

  @Test
  void iteratesAllElementsAcrossScrolls() {
    ScrollIterator<Integer> iterator = ScrollIterator.of(position -> {
      int from = position == null ? 0 : (int) position.cursor().get(0).value() + 1;
      int to = Math.min(from + 2, VALUES.size());
      return window(VALUES.subList(from, to), to >= VALUES.size());
    });

    List<Integer> collected = new ArrayList<>();
    while (iterator.hasNext()) {
      collected.add(iterator.next());
    }

    assertThat(collected).containsExactlyElementsOf(VALUES);
  }

  @Test
  void emptyFirstScrollEndsIteration() {
    ScrollIterator<Integer> iterator = ScrollIterator.of(position -> window(List.of(), true));

    assertThat(iterator.hasNext()).isFalse();
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void resumesFromGivenPosition() {
    ScrollIterator<Integer> iterator = ScrollIterator.of(position -> {
      int from = (int) position.cursor().get(0).value() + 1;
      int to = Math.min(from + 2, VALUES.size());
      return window(VALUES.subList(from, to), to >= VALUES.size());
    }, ScrollPosition.keyset(List.of(new ScrollPosition.Entry("index", 1, Order.ASC))));

    List<Integer> collected = new ArrayList<>();
    while (iterator.hasNext()) {
      collected.add(iterator.next());
    }

    assertThat(collected).containsExactly(2, 3, 4);
  }

  private static Scroll<Integer> window(List<Integer> rows, boolean last) {
    return new Scroll<>() {
      @Override
      public List<Integer> rows() {
        return rows;
      }

      @Override
      public ScrollPosition positionAt(int index) {
        return ScrollPosition.keyset(List.of(new ScrollPosition.Entry("index", rows.get(index), Order.ASC)));
      }

      @Override
      public boolean isLast() {
        return last;
      }
    };
  }

}
