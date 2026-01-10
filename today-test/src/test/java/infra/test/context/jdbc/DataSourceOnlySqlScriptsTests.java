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

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.jdbc.JdbcTestUtils;
import infra.test.transaction.TransactionAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link Sql @Sql} support with only a {@link DataSource}
 * present in the context (i.e., no transaction manager).
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({ "schema.sql", "data.sql" })
@DirtiesContext
class DataSourceOnlySqlScriptsTests {

  private JdbcTemplate jdbcTemplate;

  @Autowired
  void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Test
  @Order(1)
  void classLevelScripts() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertNumUsers(1);
  }

  @Test
  @Sql({ "drop-schema.sql", "schema.sql", "data.sql", "data-add-dogbert.sql" })
  @Order(2)
  void methodLevelScripts() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertNumUsers(2);
  }

  protected void assertNumUsers(int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "user")).as(
            "Number of rows in the 'user' table.").isEqualTo(expected);
  }

  @Configuration
  static class Config {

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()//
              .setName("empty-sql-scripts-without-tx-mgr-test-db")//
              .build();
    }
  }

}
