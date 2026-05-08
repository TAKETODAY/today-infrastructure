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

package infra.jdbc.docker.compose;

import infra.docker.compose.service.connection.test.DockerComposeTest;
import infra.jdbc.config.DatabaseDriver;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.SimpleDriverDataSource;
import infra.test.testcontainers.TestImage;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClickHouseJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ClickHouseJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

  @DockerComposeTest(composeFile = "clickhouse-compose.yaml", image = TestImage.CLICKHOUSE)
  void runCreatesConnectionDetails(JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
    assertConnectionDetails(connectionDetails);
    checkDatabaseAccess(connectionDetails);
  }

  private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
    assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
    assertThat(connectionDetails.getPassword()).isEqualTo("secret");
    assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:clickhouse://").endsWith("/mydatabase");
  }

  private void checkDatabaseAccess(JdbcConnectionDetails connectionDetails) throws ClassNotFoundException {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setUrl(connectionDetails.getJdbcUrl());
    dataSource.setUsername(connectionDetails.getUsername());
    dataSource.setPassword(connectionDetails.getPassword());
    dataSource.setDriverClass(ClassUtils.forName(connectionDetails.getDriverClassName(),
            getClass().getClassLoader()));
    JdbcTemplate template = new JdbcTemplate(dataSource);
    String validationQuery = DatabaseDriver.CLICKHOUSE.getValidationQuery();
    assertThat(validationQuery).isNotNull();
    assertThat(template.queryForObject(validationQuery, Integer.class)).isEqualTo(1);
  }

}
