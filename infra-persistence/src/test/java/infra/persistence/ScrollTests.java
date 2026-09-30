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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ScrollTests {

  @Test
  void nextPositionIsPositionOfLastRowWhenMoreRowsFollow() {
    Scroll<Integer> scroll = window(List.of(1, 2), false);

    assertThat(scroll.nextPosition()).isEqualTo(scroll.position());
    assertThat(scroll.nextPosition().cursor().get(0).value()).isEqualTo(2);
  }

  @Test
  void nextPositionIsNullOnLastWindow() {
    Scroll<Integer> scroll = window(List.of(1, 2), true);

    assertThat(scroll.nextPosition()).isNull();
  }

  @Test
  void nextPositionIsNullOnEmptyWindow() {
    assertThat(window(List.of(), false).nextPosition()).isNull();
    assertThat(window(List.of(), true).nextPosition()).isNull();
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
