/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/11 00:01
 */
class QueryConditionTests {

  @Test
  void and() {
    QueryCondition condition = QueryCondition.equalsTo("name", "T");

    StringBuilder sql = new StringBuilder();
    condition.render(sql);
    assertThat(sql.toString()).isEqualTo(" `name` = ?");

    condition.and(QueryCondition.isNull("age"));

    sql = new StringBuilder();
    condition.render(sql);
    assertThat(sql.toString()).isEqualTo(" `name` = ? AND `age` is null");

    condition.and(
            QueryCondition.between("age", new Date(), new Date())
    );

    sql = new StringBuilder();
    condition.render(sql);

    System.out.println(sql);

    assertThat(sql.toString()).isEqualTo(" `name` = ? AND `age` BETWEEN ? AND ?");
  }

}