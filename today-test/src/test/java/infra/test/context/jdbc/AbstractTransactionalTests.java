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

package infra.test.context.jdbc;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.jdbc.JdbcTestUtils;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@Transactional
public abstract class AbstractTransactionalTests {

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  protected final int countRowsInTable(String tableName) {
    return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
  }

  protected final void assertNumUsers(int expected) {
    assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
  }

  protected final void assertUsers(String... expectedUsers) {
    List<String> actualUsers = this.jdbcTemplate.queryForList("select name from user", String.class);
    assertThat(actualUsers).containsExactlyInAnyOrder(expectedUsers);
  }

}
