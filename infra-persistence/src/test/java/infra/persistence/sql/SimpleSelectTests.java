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

import infra.persistence.Order;
import infra.persistence.platform.Platform;

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