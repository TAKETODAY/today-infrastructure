/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.flyway.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.flyway.config.FlywayConnectionDetails;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@JUnitConfig(FlywayContainerServiceConnectionTests.TestConfiguration.class)
class FlywayContainerServiceConnectionTests {

  @ServiceConnection(type = FlywayConnectionDetails.class)
  static final JdbcDatabaseContainer<?> database = database();

  @Autowired
  private FlywayConnectionDetails details;

  @Test
  void publishesJdbcSettingsFromServiceConnection() {
    assertThat(this.details.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/app");
    assertThat(this.details.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(this.details.getUsername()).isEqualTo("migrator");
    assertThat(this.details.getPassword()).isEqualTo("test-password");
  }

  private static JdbcDatabaseContainer<?> database() {
    JdbcDatabaseContainer<?> database = mock();
    given(database.getDockerImageName()).willReturn("postgres:17");
    given(database.isRunning()).willReturn(true);
    given(database.getJdbcUrl()).willReturn("jdbc:postgresql://localhost:5432/app");
    given(database.getDriverClassName()).willReturn("org.postgresql.Driver");
    given(database.getUsername()).willReturn("migrator");
    given(database.getPassword()).willReturn("test-password");
    return database;
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {
  }
}
