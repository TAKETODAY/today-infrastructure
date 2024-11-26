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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which verify that scripts executed via {@link Sql @Sql}
 * will persist between non-transactional test methods.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({ "schema.sql", "data.sql" })
@DirtiesContext
class NonTransactionalSqlScriptsTests {

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Test
  @Order(1)
  void classLevelScripts() {
    assertNumUsers(1);
  }

  @Test
  @Sql("data-add-dogbert.sql")
  @Order(2)
  void methodLevelScripts() {
    assertNumUsers(2);
  }

  void assertNumUsers(int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
  }

}
