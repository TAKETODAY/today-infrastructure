/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/24 14:53
 */
class TripleTests {

  @Test
  void test() {
    Triple<Integer, Integer, String> triple = new Triple<>(1, 2, "3");
    assertThat(triple.first).isEqualTo(1);
    assertThat(triple.second).isEqualTo(2);
    assertThat(triple.third).isEqualTo("3");

    Triple<Integer, Integer, String> triple2 = Triple.of(1, 2, "3");

    assertThat(triple.third).isEqualTo(triple2.third);
    assertThat(triple.first).isEqualTo(triple2.first);
    assertThat(triple.second).isEqualTo(triple2.second);

    assertThat(triple.withFirst(1)).isSameAs(triple);
    assertThat(triple.withFirst(2)).isNotEqualTo(triple);

    assertThat(triple.withSecond(2)).isSameAs(triple);
    assertThat(triple.withSecond(1)).isNotEqualTo(triple);

    assertThat(triple.withThird("3")).isSameAs(triple);
    assertThat(triple.withThird("1")).isNotEqualTo(triple);

    assertThat(triple).isNotEqualTo(1);
    assertThat(triple).isEqualTo(triple);

    var triplePair = Triple.of(1, Pair.of(1, 2), 3);

    assertThat(triplePair.second).isEqualTo(Pair.of(1, 2)).isNotEqualTo(Pair.of(2, 2));

    Set<Triple<Integer, ?, ?>> tripleSet = CollectionUtils.newHashSet(triple, triple2, triplePair, triplePair.withFirst(1));

    assertThat(tripleSet).hasSize(2);

    assertThat(triplePair.second.first).isEqualTo(1);

    //
    Pair<Integer, Integer> pair = Pair.of(1, 2);

    assertThat(pair.first).isEqualTo(1);
    assertThat(pair.second).isEqualTo(2);

    assertThat(pair.withFirst(1)).isEqualTo(Pair.of(1, 2)).isSameAs(pair);
    assertThat(pair.withFirst(2)).isEqualTo(Pair.of(2, 2));

    assertThat(pair.withSecond(2)).isEqualTo(Pair.of(1, 2)).isSameAs(pair);
    assertThat(pair.withSecond(3)).isEqualTo(Pair.of(1, 3)).isNotEqualTo(pair);

    assertThat(pair).isNotEqualTo(triple);
    assertThat(pair).isEqualTo(pair);

    HashSet<Pair<Integer, Integer>> pairs = CollectionUtils.newHashSet(pair, pair.withFirst(1), pair.withFirst(2), pair.withSecond(1));

    assertThat(pairs).hasSize(3);

    assertThat(pair.toString()).contains("1", "2");
    assertThat(triple.toString()).contains("1", "2");
  }

}