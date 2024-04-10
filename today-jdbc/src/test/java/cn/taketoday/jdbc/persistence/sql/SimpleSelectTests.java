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

import cn.taketoday.jdbc.persistence.Order;
import cn.taketoday.jdbc.persistence.dialect.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 21:16
 */
class SimpleSelectTests {

  private final Platform platform = Platform.forClasspath();

  @Test
  void simple() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addColumns(new String[] { "id", "gender" })
            .setTableName("t_user");

    assertThat(select.toStatementString(platform)).isEqualTo("SELECT `name`, `age`, `id`, `gender` FROM t_user");
  }

  @Test
  void alias() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addColumn("user_id", "id")
            .addColumn("user_id", "id")
            .setTableName("t_user");

    assertThat(select.toStatementString(platform)).isEqualTo("SELECT `name`, `age`, `user_id` AS id FROM t_user");
  }

  @Test
  void where() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addColumn("user_id", "id")
            .addColumn("user_id", "id")
            .addWhereToken("id = 1")
            .addRestrictions("name", "gender")
            .addRestriction(Restriction.notEqual("age", "1"))
            .setTableName("t_user");

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT `name`, `age`, `user_id` AS id FROM t_user WHERE id = 1 AND `name` = ? AND `gender` = ? AND `age` <> 1");
  }

  @Test
  void orderBy() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addWhereToken("id = 1")
            .addRestriction("name")
            .setTableName("t_user")
            .orderBy("id", Order.DESC);

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT `name`, `age` FROM t_user WHERE id = 1 AND `name` = ? order by `id` DESC");
  }

  @Test
  void comment() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addWhereToken("id = 1")
            .setTableName("t_user")
            .setComment("find by id")
            .orderBy("id");

    assertThat(select.toStatementString(platform)).isEqualTo(
            "/* find by id */ SELECT `name`, `age` FROM t_user WHERE id = 1 order by `id` ASC");
  }

}