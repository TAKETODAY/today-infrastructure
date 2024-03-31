/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence.sql;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.core.Pair;
import cn.taketoday.jdbc.persistence.Order;

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