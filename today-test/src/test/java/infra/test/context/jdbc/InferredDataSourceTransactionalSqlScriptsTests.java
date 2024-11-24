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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.Transactional;

import static infra.test.transaction.TransactionAssert.assertThatTransaction;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact copy of {@link InferredDataSourceSqlScriptsTests}, except that test
 * methods are transactional.
 *
 * @author Sam Brannen
 * @see InferredDataSourceSqlScriptsTests
 * @since 4.0
 */
@JUnitConfig
@DirtiesContext
class InferredDataSourceTransactionalSqlScriptsTests {

  @Autowired
  DataSource dataSource1;

  @Autowired
  DataSource dataSource2;

  @Test
  @Transactional("txMgr1")
  @Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionManager = "txMgr1"))
  void database1() {
    assertThatTransaction().isActive();
    assertUsers(new JdbcTemplate(dataSource1), "Dilbert", "Dogbert");
  }

  @Test
  @Transactional("txMgr2")
  @Sql(scripts = "data-add-catbert.sql", config = @SqlConfig(transactionManager = "txMgr2"))
  void database2() {
    assertThatTransaction().isActive();
    assertUsers(new JdbcTemplate(dataSource2), "Dilbert", "Catbert");
  }

  private void assertUsers(JdbcTemplate jdbcTemplate, String... users) {
    List<String> expected = Arrays.asList(users);
    Collections.sort(expected);
    List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
    Collections.sort(actual);
    assertThat(actual).as("Users in database;").isEqualTo(expected);
  }

  @Configuration
  static class Config {

    @Bean
    PlatformTransactionManager txMgr1() {
      return new DataSourceTransactionManager(dataSource1());
    }

    @Bean
    PlatformTransactionManager txMgr2() {
      return new DataSourceTransactionManager(dataSource2());
    }

    @Bean
    DataSource dataSource1() {
      return new EmbeddedDatabaseBuilder()//
              .setName("database1")//
              .addScript("classpath:/infra/test/context/jdbc/schema.sql")//
              .addScript("classpath:/infra/test/context/jdbc/data.sql")//
              .build();
    }

    @Bean
    DataSource dataSource2() {
      return new EmbeddedDatabaseBuilder()//
              .setName("database2")//
              .addScript("classpath:/infra/test/context/jdbc/schema.sql")//
              .addScript("classpath:/infra/test/context/jdbc/data.sql")//
              .build();
    }
  }

}
