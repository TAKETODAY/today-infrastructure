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

import infra.core.Pair;
import infra.persistence.Order;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:53
 */
class OrderSpecTests {

  private final Platform platform = Platform.generic();

  @Test
  void asc() {
    assertThat(OrderSpec.asc("name").toClause(platform).toString()).isEqualTo("name ASC");
  }

  @Test
  void desc() {
    assertThat(OrderSpec.desc("name").toClause(platform).toString()).isEqualTo("name DESC");
  }

  @Test
  void isEmpty() {
    assertThat(OrderSpec.builder().build().isEmpty()).isTrue();
    assertThat(OrderSpec.asc("name").isEmpty()).isFalse();
    assertThat(OrderSpec.plain("   ").isEmpty()).isTrue();
  }

  @Test
  void empty() {
    assertThat(OrderSpec.empty().isEmpty()).isTrue();
    assertThat(OrderSpec.empty().toClause(platform).toString()).isEmpty();
    assertThat(OrderSpec.builder().build()).isSameAs(OrderSpec.empty());
  }

  @Test
  void equalsAndHashCode() {
    assertThat(OrderSpec.asc("name")).isEqualTo(OrderSpec.asc("name"));
    assertThat(OrderSpec.asc("name")).hasSameHashCodeAs(OrderSpec.asc("name"));
    assertThat(OrderSpec.asc("name")).isEqualTo(OrderSpec.of(Pair.of("name", Order.ASC)));
    assertThat(OrderSpec.asc("name")).isNotEqualTo(OrderSpec.desc("name"));
    assertThat(OrderSpec.plain("name ASC")).isEqualTo(OrderSpec.plain("name ASC"));
    assertThat(OrderSpec.plain("name ASC")).isNotEqualTo(OrderSpec.empty());
    assertThat(OrderSpec.of(Pair.of("name", Order.ASC), Pair.of("age", Order.DESC)))
            .isEqualTo(OrderSpec.builder().asc("name").desc("age").build());
    assertThat(OrderSpec.of(Pair.of("name", Order.ASC), Pair.of("age", Order.DESC)))
            .isNotEqualTo(OrderSpec.of(Pair.of("age", Order.DESC), Pair.of("name", Order.ASC)));
  }

  @Test
  void of() {
    OrderSpec orderSpec = OrderSpec.of(Pair.of("name", Order.ASC), Pair.of("age", Order.DESC));
    assertThat(orderSpec.toClause(platform).toString()).isEqualTo("name ASC, age DESC");
  }

  @Test
  void builder() {
    OrderSpec orderSpec = OrderSpec.builder()
            .desc("name")
            .asc("age")
            .build();
    assertThat(orderSpec.toClause(platform).toString()).isEqualTo("name DESC, age ASC");
  }

  @Test
  void builderIsImmutableAfterBuild() {
    OrderSpec.Builder builder = OrderSpec.builder().asc("name");
    OrderSpec orderSpec = builder.build();
    builder.asc("age");

    assertThat(orderSpec.toClause(platform).toString()).isEqualTo("name ASC");
  }

  @Test
  void plain() {
    assertThat(OrderSpec.plain("`name` ASC, `age` DESC").isEmpty()).isFalse();
    assertThat(OrderSpec.plain("`name` ASC, `age` DESC").toClause(platform))
            .isEqualTo("`name` ASC, `age` DESC");
  }

}
