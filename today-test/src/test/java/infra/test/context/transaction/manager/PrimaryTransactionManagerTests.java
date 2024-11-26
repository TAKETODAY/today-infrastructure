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
