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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:55
 */
class TableTests {

  @Test
  void shouldCreateTableWithValidParameters() {
    String name = "users";
    List<Row> rows = List.of();
    List<Column> columns = List.of();

    Table table = new Table(name, rows, columns);

    assertThat(table).isNotNull();
    assertThat(table.getName()).isEqualTo(name);
  }

  @Test
  void shouldGetTableName() {
    String name = "products";
    Table table = new Table(name, List.of(), List.of());

    assertThat(table.getName()).isEqualTo(name);
  }

  @Test
  void shouldGetColumns() {
    Column column1 = new Column("id", 1, "INTEGER");
    Column column2 = new Column("name", 2, "VARCHAR");
    List<Column> columns = List.of(column1, column2);

    Table table = new Table("users", List.of(), columns);

    assertThat(table.columns()).isEqualTo(columns);
    assertThat(table.columns()).hasSize(2);
  }

  @Test
  void shouldReturnEmptyListWhenNoRows() {
    Table table = new Table("empty", List.of(), List.of());
    List<Map<String, Object>> mapList = table.asList();

    assertThat(mapList).isNotNull();
    assertThat(mapList).isEmpty();
  }

}