/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.jdbc.JdbcTestUtils;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.EnableTransactionManagement;
import cn.taketoday.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that ensure that <em>primary</em> transaction managers
 * are supported.
 *
 * @author Sam Brannen
 * @see cn.taketoday.test.context.jdbc.PrimaryDataSourceTests
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

    ClassPathResource resource = new ClassPathResource("/cn/taketoday/test/context/jdbc/data.sql");
    new ResourceDatabasePopulator(resource).execute(jdbcTemplate.getDataSource());

    assertNumUsers(1);
  }

  private void assertNumUsers(int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user")).as("Number of rows in the 'user' table").isEqualTo(expected);
  }

  @Configuration
  @EnableTransactionManagement  // SPR-17137: should not break trying to proxy the final test class
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
              .addScript("classpath:/cn/taketoday/test/context/jdbc/schema.sql")
              .build();
    }

    @Bean
    DataSource dataSource2() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
    }

  }

}
