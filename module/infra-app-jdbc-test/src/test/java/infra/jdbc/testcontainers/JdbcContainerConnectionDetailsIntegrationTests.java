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

package infra.jdbc.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.jdbc.test.config.TestDatabaseAutoConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@JUnitConfig(JdbcContainerConnectionDetailsIntegrationTests.Config.class)
@TestPropertySource(properties = "datasource.type=infra.jdbc.datasource.DriverManagerDataSource")
class JdbcContainerConnectionDetailsIntegrationTests {

  private static final String JDBC_URL = "jdbc:h2:mem:service-connection";

  @ServiceConnection(type = JdbcConnectionDetails.class)
  static final JdbcDatabaseContainer<?> database = mockDatabase();

  @Test
  void autoConfiguredDataSourceUsesContainerConnectionDetails(@Autowired DriverManagerDataSource dataSource,
          @Autowired JdbcConnectionDetails details) {
    assertThat(details.getDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(dataSource.getUrl()).isEqualTo(JDBC_URL);
    assertThat(dataSource.getUsername()).isEqualTo("test-user");
    assertThat(dataSource.getPassword()).isEqualTo("test-password");
  }

  private static JdbcDatabaseContainer<?> mockDatabase() {
    JdbcDatabaseContainer<?> container = mock(JdbcDatabaseContainer.class);
    given(container.getDockerImageName()).willReturn("example/database:1");
    given(container.isRunning()).willReturn(true);
    given(container.getJdbcUrl()).willReturn(JDBC_URL);
    given(container.getDriverClassName()).willReturn("org.h2.Driver");
    given(container.getUsername()).willReturn("test-user");
    given(container.getPassword()).willReturn("test-password");
    return container;
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({ TestDatabaseAutoConfiguration.class, DataSourceAutoConfiguration.class })
  static class Config {

  }

}
