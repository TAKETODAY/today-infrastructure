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
 * @since 1.7.1 2024/4/8 16:29
 */
class InsertTests {

  @Test
  void addColumn() {
    Insert insert = new Insert("t_user");
    insert.setComment("comment");

    insert.addColumn("name");
    insert.addColumn("age");

    assertThat(insert.toStatementString(Platform.generic()))
            .isEqualTo("/* comment */ INSERT INTO t_user (`name`, `age`) VALUES (?, ?)");
  }

  @Test
  void addColumns() {
    Insert insert = new Insert("t_user");
    insert.setComment("comment");

    insert.addColumns(new String[] { "name", "age" });

    assertThat(insert.toStatementString(Platform.generic()))
            .isEqualTo("/* comment */ INSERT INTO t_user (`name`, `age`) VALUES (?, ?)");
  }

  @Test
  void empty() {
    Insert insert = new Insert("t_user");
    insert.setComment("comment");

    assertThat(insert.toStatementString(Platform.generic()))
            .isEqualTo("/* comment */ INSERT INTO t_user VALUES ( )");
  }

}