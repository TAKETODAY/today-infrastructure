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

package cn.taketoday.test.context.configuration.interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.context.jdbc.SqlConfig;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
class SqlConfigInterfaceTests implements SqlConfigTestInterface {

  JdbcTemplate jdbcTemplate;

  @Autowired
  void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Test
  @Sql(scripts = "/cn/taketoday/test/context/jdbc/schema.sql", //
       config = @SqlConfig(separator = ";"))
  @Sql("/cn/taketoday/test/context/jdbc/data-add-users-with-custom-script-syntax.sql")
  void methodLevelScripts() {
    assertNumUsers(3);
  }

  void assertNumUsers(int expected) {
    assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
  }

  int countRowsInTable(String tableName) {
    return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
  }

}
