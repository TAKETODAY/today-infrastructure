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

package infra.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.core.io.ClassPathResource;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;
import infra.test.jdbc.JdbcTestUtils;
import infra.test.transaction.TransactionAssert;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that ensure that <em>primary</em> transaction managers
 * are supported.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@DirtiesContext
final /* Intentionally FINAL */ class PrimaryTransactionManagerTests {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  PrimaryTransactionManagerTests(DataSource dataSource1) {
    this.jdbcTemplate = new JdbcTemplate(dataSource1);
  }

  @BeforeTransaction
  void beforeTransaction() {
    assertNumUsers(0);
  }

  @AfterTransaction
  void afterTransaction() {
    assertNumUsers(0);
  }

  @Test
  @Transactional
  void transactionalTest() {
    TransactionAssert.assertThatTransaction().isActive();

    ClassPathResource resource = new ClassPathResource("/infra/test/context/jdbc/data.sql");
    new ResourceDatabasePopulator(resource).execute(jdbcTemplate.getDataSource());

    assertNumUsers(1);
  }

  private void assertNumUsers(int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user")).as("Number of rows in the 'user' table").isEqualTo(expected);
  }

  @Configuration
  @EnableTransactionManagement
  static class Config {

    @Primary
    @Bean
    PlatformTransactionManager primaryTransactionManager() {
      return new DataSourceTransactionManager(dataSource1());
    }

    @Bean
    PlatformTransactionManager additionalTransactionManager() {
      return new DataSourceTransactionManager(dataSource2());
    }

    @Bean
    DataSource dataSource1() {
      return new EmbeddedDatabaseBuilder()
              .generateUniqueName(true)
              .addScript("classpath:/infra/test/context/jdbc/schema.sql")
              .build();
    }

    @Bean
    DataSource dataSource2() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
    }

  }

}
