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
