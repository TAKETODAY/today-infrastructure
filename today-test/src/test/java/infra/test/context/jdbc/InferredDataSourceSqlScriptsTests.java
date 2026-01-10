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
import infra.test.transaction.TransactionAssert;
import infra.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link Sql @Sql} that verify support for inferring
 * {@link DataSource}s from {@link PlatformTransactionManager}s.
 *
 * @author Sam Brannen
 * @see InferredDataSourceTransactionalSqlScriptsTests
 * @since 4.0
 */
@JUnitConfig
@DirtiesContext
class InferredDataSourceSqlScriptsTests {

  @Autowired
  DataSource dataSource1;

  @Autowired
  DataSource dataSource2;

  @Test
  @Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionManager = "txMgr1"))
  void database1() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertUsers(new JdbcTemplate(dataSource1), "Dilbert", "Dogbert");
  }

  @Test
  @Sql(scripts = "data-add-catbert.sql", config = @SqlConfig(transactionManager = "txMgr2"))
  void database2() {
    TransactionAssert.assertThatTransaction().isNotActive();
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
