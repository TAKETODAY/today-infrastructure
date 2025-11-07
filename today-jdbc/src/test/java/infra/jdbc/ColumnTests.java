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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:57
 */
class ColumnTests {

  @Test
  void shouldCreateColumnWithValidParameters() {
    String name = "user_name";
    int index = 1;
    String type = "VARCHAR";

    Column column = new Column(name, index, type);

    assertThat(column).isNotNull();
    assertThat(column.getName()).isEqualTo(name);
    assertThat(column.getIndex()).isEqualTo(index);
    assertThat(column.getType()).isEqualTo(type);
  }

  @Test
  void shouldGetName() {
    String name = "id";
    Column column = new Column(name, 2, "INTEGER");

    assertThat(column.getName()).isEqualTo(name);
  }

  @Test
  void shouldGetIndex() {
    int index = 3;
    Column column = new Column("email", index, "VARCHAR");

    assertThat(column.getIndex()).isEqualTo(index);
  }

  @Test
  void shouldGetType() {
    String type = "TIMESTAMP";
    Column column = new Column("created_at", 4, type);

    assertThat(column.getType()).isEqualTo(type);
  }

  @Test
  void shouldToStringReturnFormattedString() {
    String name = "status";
    String type = "BOOLEAN";
    Column column = new Column(name, 5, type);

    String result = column.toString();

    assertThat(result).isEqualTo(name + " (" + type + ")");
  }

  @Test
  void shouldHandleNullName() {
    String name = null;
    String type = "TEXT";
    Column column = new Column(name, 6, type);

    assertThat(column.getName()).isNull();
    assertThat(column.toString()).isEqualTo("null (" + type + ")");
  }

  @Test
  void shouldHandleNullType() {
    String name = "description";
    String type = null;
    Column column = new Column(name, 7, type);

    assertThat(column.getType()).isNull();
    assertThat(column.toString()).isEqualTo(name + " (null)");
  }

  @Test
  void shouldHandleZeroIndex() {
    int index = 0;
    Column column = new Column("zero_index", index, "VARCHAR");

    assertThat(column.getIndex()).isEqualTo(index);
  }

  @Test
  void shouldHandleNegativeIndex() {
    int index = -1;
    Column column = new Column("negative_index", index, "VARCHAR");

    assertThat(column.getIndex()).isEqualTo(index);
  }

}