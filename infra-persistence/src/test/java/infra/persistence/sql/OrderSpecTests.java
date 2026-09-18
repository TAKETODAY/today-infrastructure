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

import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:53
 */
class OrderSpecTests {

  @Test
  void asc() {
    assertThat(OrderSpec.mutable()
            .asc("name").toClause(Platform.generic()).toString()).isEqualTo("name ASC");
  }

  @Test
  void isEmpty() {
    assertThat(OrderSpec.mutable().isEmpty()).isTrue();
    assertThat(OrderSpec.mutable().asc("name").isEmpty()).isFalse();
  }

  @Test
  void merge() {
    MutableOrderSpec clause = new MutableOrderSpec().asc("name");
    clause.merge(OrderSpec.mutable().desc("age"));
    assertThat(clause.isEmpty()).isFalse();
    assertThat(clause.toClause(Platform.generic()).toString()).isEqualTo("name ASC, age DESC");
  }

  @Test
  void plain() {
    assertThat(OrderSpec.plain("`name` ASC, `age` DESC").isEmpty()).isFalse();
    assertThat(OrderSpec.plain("`name` ASC, `age` DESC").toClause(Platform.generic())).isEqualTo("`name` ASC, `age` DESC");
  }

}