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

package infra.test.context.configuration.interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.context.jdbc.Sql;
import infra.test.context.jdbc.SqlConfig;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.jdbc.JdbcTestUtils;

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
  @Sql(scripts = "/infra/test/context/jdbc/schema.sql", //
       config = @SqlConfig(separator = ";"))
  @Sql("/infra/test/context/jdbc/data-add-users-with-custom-script-syntax.sql")
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
