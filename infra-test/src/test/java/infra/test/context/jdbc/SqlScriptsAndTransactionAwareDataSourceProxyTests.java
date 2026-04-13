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

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.TransactionAwareDataSourceProxy;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.transaction.PlatformTransactionManager;

/**
 * Transactional integration tests for {@link Sql @Sql} support when the
 * {@link DataSource} is wrapped in a {@link TransactionAwareDataSourceProxy}.
 *
 * @author Sam Brannen
 */
@JUnitConfig
@DirtiesContext
class SqlScriptsAndTransactionAwareDataSourceProxyTests extends AbstractTransactionalTests {

  @Test
  @Sql("data-add-catbert.sql")
  void onlyCatbertIsPresent() {
    assertUsers("Catbert");
  }

  @Test
  @Sql("data-add-dogbert.sql")
  void onlyDogbertIsPresent() {
    assertUsers("Dogbert");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    DataSource dataSource() {
      DataSource dataSource = new EmbeddedDatabaseBuilder()
              .generateUniqueName(true)
              .addScript("classpath:/infra/test/context/jdbc/schema.sql")
              .build();
      return new TransactionAwareDataSourceProxy(dataSource);
    }
  }

}
