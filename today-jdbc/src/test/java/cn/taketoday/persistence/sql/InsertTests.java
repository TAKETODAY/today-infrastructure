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

package cn.taketoday.persistence.sql;

import org.junit.jupiter.api.Test;

import cn.taketoday.persistence.dialect.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.7.1 2024/4/8 16:29
 */
class InsertTests {

  @Test
  void addColumn() {
    Insert insert = new Insert("t_user");
    insert.setComment("comment");

    insert.addColumn("name");
    insert.addColumn("age");

    assertThat(insert.toStatementString(Platform.forClasspath()))
            .isEqualTo("/* comment */ INSERT INTO t_user (`name`, `age`) VALUES (?, ?)");
  }

  @Test
  void addColumns() {
    Insert insert = new Insert("t_user");
    insert.setComment("comment");

    insert.addColumns(new String[] { "name", "age" });

    assertThat(insert.toStatementString(Platform.forClasspath()))
            .isEqualTo("/* comment */ INSERT INTO t_user (`name`, `age`) VALUES (?, ?)");
  }

  @Test
  void empty() {
    Insert insert = new Insert("t_user");
    insert.setComment("comment");

    assertThat(insert.toStatementString(Platform.forClasspath()))
            .isEqualTo("/* comment */ INSERT INTO t_user VALUES ( )");
  }

}