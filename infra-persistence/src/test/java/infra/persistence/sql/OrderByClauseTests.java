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

package infra.persistence.sql;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.core.Pair;
import infra.persistence.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:53
 */
class OrderByClauseTests {

  @Test
  void toClause() {
    OrderByClause clause = OrderByClause.forMap(Map.of("name", Order.ASC));
    assertThat(clause.toClause().toString()).isEqualTo("`name` ASC");

    assertThat(OrderByClause.valueOf(Pair.of("name", Order.ASC), Pair.of("age", Order.DESC))
            .toClause().toString()).isEqualTo("`name` ASC, `age` DESC");
  }

  @Test
  void desc() {
    assertThat(OrderByClause.valueOf()
            .desc("name").toClause().toString()).isEqualTo("`name` DESC");
  }

  @Test
  void asc() {
    assertThat(OrderByClause.mutable()
            .asc("name").toClause().toString()).isEqualTo("`name` ASC");
  }

  @Test
  void isEmpty() {
    assertThat(OrderByClause.mutable().isEmpty()).isTrue();
    assertThat(OrderByClause.mutable().asc("name").isEmpty()).isFalse();
  }

  @Test
  void merge() {
    MutableOrderByClause clause = new MutableOrderByClause().asc("name");
    clause.merge(OrderByClause.mutable().desc("age"));
    assertThat(clause.isEmpty()).isFalse();
    assertThat(clause.toClause().toString()).isEqualTo("`name` ASC, `age` DESC");
  }

  @Test
  void plain() {
    assertThat(OrderByClause.plain("`name` ASC, `age` DESC").isEmpty()).isFalse();
    assertThat(OrderByClause.plain("`name` ASC, `age` DESC").toClause()).isEqualTo("`name` ASC, `age` DESC");
  }

}