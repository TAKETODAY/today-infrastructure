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

import javax.sql.DataSource;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.transaction.PlatformTransactionManager;

/**
 * Database configuration class for SQL script integration tests with the 'user'
 * table already created.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Configuration
public class PopulatedSchemaDatabaseConfig {

  @Bean
  PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()//
            .generateUniqueName(true)
            .addScript("classpath:/infra/test/context/jdbc/schema.sql") //
            .build();
  }

  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

}
