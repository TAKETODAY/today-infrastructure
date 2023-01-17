/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/9 22:29
 */
class UpdateTests {

  @Test
  void sql() {
    Update update = new Update();
    update.setTableName("t_user");

    update.addColumn("name");
    update.setVersionColumnName("version");
    update.setPrimaryKeyColumnNames("id");

    assertThat(update.toStatementString()).isEqualTo("update t_user set name=? where id=? and version=?");

    update.addColumn("name", ":name");
    assertThat(update.toStatementString()).isEqualTo("update t_user set name=:name where id=? and version=?");
  }

}