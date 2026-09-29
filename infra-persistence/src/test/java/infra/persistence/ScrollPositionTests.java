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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScrollPositionTests {

  @Test
  void initialPositionHasNoCursor() {
    assertThat(ScrollPosition.INITIAL.isInitial()).isTrue();
    assertThat(ScrollPosition.INITIAL.cursor()).isNull();

    ScrollPosition withOrder = ScrollPosition.builder().desc("age", null).asc("id", null).build();
    assertThat(withOrder.isInitial()).isTrue();
    assertThat(withOrder.cursor()).containsExactly(
            new ScrollPosition.Entry("age", null, Order.DESC),
            new ScrollPosition.Entry("id", null, Order.ASC));
  }

  @Test
  void builderPreservesKeyOrderDirectionAndValues() {
    ScrollPosition position = ScrollPosition.builder().desc("age", 20).add("name", "Alice", Order.ASC)
            .asc("id", 3).build();

    assertThat(position.isInitial()).isFalse();
    assertThat(position.cursor()).containsExactly(
            new ScrollPosition.Entry("age", 20, Order.DESC),
            new ScrollPosition.Entry("name", "Alice", Order.ASC),
            new ScrollPosition.Entry("id", 3, Order.ASC));
  }

  @Test
  void positionSnapshotsBuilderAndInputList() {
    ScrollPosition.Builder builder = ScrollPosition.builder().asc("age", 20);
    ScrollPosition snapshot = builder.build();
    builder.desc("id", 3);
    assertThat(snapshot.cursor()).containsExactly(new ScrollPosition.Entry("age", 20, Order.ASC));
    assertThat(builder.build().cursor()).hasSize(2);

    List<ScrollPosition.Entry> entries = new ArrayList<>();
    entries.add(new ScrollPosition.Entry("id", 1, Order.ASC));
    ScrollPosition position = ScrollPosition.keyset(entries);
    entries.add(new ScrollPosition.Entry("age", 20, Order.DESC));
    assertThat(position.cursor()).containsExactly(new ScrollPosition.Entry("id", 1, Order.ASC));
    assertThatThrownBy(() -> position.cursor().add(new ScrollPosition.Entry("age", 20, Order.DESC)))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsInvalidCursorValuesAndEntries() {
    assertThatThrownBy(() -> ScrollPosition.builder().build()).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ScrollPosition.keyset(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ScrollPosition.builder().asc("age", null).asc("id", 1).build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ScrollPosition.builder().asc("age", 20).asc("id", null).build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ScrollPosition.builder().asc(" ", 20))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ScrollPosition.builder().add("age", 20, null))
            .isInstanceOf(IllegalArgumentException.class);
  }

}
