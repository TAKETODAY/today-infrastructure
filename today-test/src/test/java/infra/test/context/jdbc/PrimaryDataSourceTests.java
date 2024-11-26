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
