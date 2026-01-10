/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
