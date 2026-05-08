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
import infra.jdbc.test.config.AutoConfigureTestDatabase.Replace;
import infra.jdbc.test.config.AutoConfigureTestDatabaseTestcontainersJdbcUrlIntegrationTests.InitializeDatasourceUrl;
import org.testcontainers.junit.jupiter.Testcontainers;

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
import infra.test.testcontainers.TestImage;
import infra.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AutoConfigureTestDatabase} with Testcontainers and a
 * {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 */
@InfraTest
@ContextConfiguration(initializers = InitializeDatasourceUrl.class)
@AutoConfigureTestDatabase(replace = Replace.NON_TEST)
@Testcontainers(disabledWithoutDocker = true)
@OverrideAutoConfiguration(enabled = false)
class AutoConfigureTestDatabaseTestcontainersJdbcUrlIntegrationTests {

  @Autowired
  private DataSource dataSource;

  @Test
  void dataSourceIsNotReplaced() {
    assertThat(this.dataSource).isInstanceOf(HikariDataSource.class).isNotInstanceOf(EmbeddedDatabase.class);
  }

  @Configuration
  @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
  static class Config {

  }

  static class InitializeDatasourceUrl implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
              "datasource.url=jdbc:tc:postgis:" + TestImage.POSTGRESQL.getTag() + ":///");
    }

  }

}
