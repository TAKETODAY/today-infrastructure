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

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.jdbc.JdbcTestUtils;
import infra.test.transaction.TransactionAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that ensure that <em>primary</em> data sources are
 * supported.
 *
 * @author Sam Brannen
 * @see infra.test.context.transaction.manager.PrimaryTransactionManagerTests
 * @since 4.0
 */
@JUnitConfig
@DirtiesContext
class PrimaryDataSourceTests {

  @Configuration
  static class Config {

    @Primary
    @Bean
    DataSource primaryDataSource() {
      // @formatter:off
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/infra/test/context/jdbc/schema.sql")
					.build();
			// @formatter:on
    }

    @Bean
    DataSource additionalDataSource() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
    }

  }

  private JdbcTemplate jdbcTemplate;

  @Autowired
  void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Test
  @Sql("data.sql")
  void dataSourceTest() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(JdbcTestUtils.countRowsInTable(this.jdbcTemplate, "user")).as("Number of rows in the 'user' table.").isEqualTo(1);
  }

}
