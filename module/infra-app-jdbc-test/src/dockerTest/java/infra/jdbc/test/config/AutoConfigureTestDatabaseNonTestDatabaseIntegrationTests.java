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

import org.junit.jupiter.api.Test;
import infra.jdbc.test.config.AutoConfigureTestDatabaseNonTestDatabaseIntegrationTests.SetupDatabase;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import infra.app.test.config.OverrideAutoConfiguration;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.test.context.ContextConfiguration;
import infra.test.context.support.TestPropertySourceUtils;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase} with Docker Compose.
 *
 * @author Phillip Webb
 */
@InfraTest
@ContextConfiguration(initializers = SetupDatabase.class)
@AutoConfigureTestDatabase
@OverrideAutoConfiguration(enabled = false)
@DisabledIfDockerUnavailable
class AutoConfigureTestDatabaseNonTestDatabaseIntegrationTests {

  @Container
  static PostgreSQLContainer postgres = TestImage.container(PostgreSQLContainer.class);

  @Autowired
  private DataSource dataSource;

  @Test
  void dataSourceIsReplaced() {
    assertThat(this.dataSource).isInstanceOf(EmbeddedDatabase.class);
  }

  @Configuration
  @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
  static class Config {

  }

  static class SetupDatabase implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      postgres.start();
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
              "datasource.url=" + postgres.getJdbcUrl());
    }

  }

}
