/*
 * Copyright 2017 - 2025 the original author or authors.
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