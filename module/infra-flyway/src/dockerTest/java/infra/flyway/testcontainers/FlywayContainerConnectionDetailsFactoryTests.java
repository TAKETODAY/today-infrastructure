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

package infra.flyway.testcontainers;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.flyway.config.FlywayAutoConfiguration;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.core.JdbcTemplate;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.testcontainers.TestImage;
import infra.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link FlywayContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@JUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class FlywayContainerConnectionDetailsFactoryTests {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer postgres = TestImage.container(PostgreSQLContainer.class);

  @Autowired(required = false)
  private JdbcConnectionDetails connectionDetails;

  @Autowired
  private Flyway flyway;

  @Test
  void connectionCanBeMadeToJdbcContainer() {
    assertThat(this.connectionDetails).isNotNull();
    JdbcTemplate jdbc = new JdbcTemplate(this.flyway.getConfiguration().getDataSource());
    assertThatNoException().isThrownBy(() -> jdbc.execute("SELECT * from public.flyway_schema_history"));
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(FlywayAutoConfiguration.class)
  static class TestConfiguration {

  }

}
