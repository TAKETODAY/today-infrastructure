/*
 * Copyright 2012-present the original author or authors.
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

package infra.jdbc.test.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import infra.app.test.config.OverrideAutoConfiguration;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.test.testcontainers.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase} with Testcontainers and a
 * {@link DynamicPropertySource @DynamicPropertySource}.
 *
 * @author Phillip Webb
 */
@InfraTest
@AutoConfigureTestDatabase
@Testcontainers(disabledWithoutDocker = true)
@OverrideAutoConfiguration(enabled = false)
class AutoConfigureTestDatabaseDynamicPropertySourceIntegrationTests {

  @Container
  static PostgreSQLContainer postgres = TestImage.container(PostgreSQLContainer.class);

  @Autowired
  private DataSource dataSource;

  @DynamicPropertySource
  static void jdbcProperties(DynamicPropertyRegistry registry) {
    registry.add("datasource.url", postgres::getJdbcUrl);
  }

  @Test
  void dataSourceIsNotReplaced() {
    assertThat(this.dataSource).isInstanceOf(HikariDataSource.class).isNotInstanceOf(EmbeddedDatabase.class);
  }

  @Configuration
  @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
  static class Config {

  }

}
