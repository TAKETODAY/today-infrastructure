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

import org.awaitility.Awaitility;

import java.time.Duration;

import infra.docker.compose.service.connection.test.DockerComposeTest;
import infra.jdbc.config.DatabaseDriver;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.SimpleDriverDataSource;
import infra.test.testcontainers.TestImage;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleFreeJdbcDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class OracleFreeJdbcDockerComposeConnectionDetailsFactoryIntegrationTests {

  @DockerComposeTest(composeFile = "oracle-compose.yaml", image = TestImage.ORACLE_FREE)
  void runCreatesConnectionDetailsThatCanBeUsedToAccessDatabase(JdbcConnectionDetails connectionDetails)
          throws Exception {
    assertThat(connectionDetails.getUsername()).isEqualTo("app_user");
    assertThat(connectionDetails.getPassword()).isEqualTo("app_user_secret");
    assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:oracle:thin:@").endsWith("/freepdb1");
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setUrl(connectionDetails.getJdbcUrl());
    dataSource.setUsername(connectionDetails.getUsername());
    dataSource.setPassword(connectionDetails.getPassword());
    dataSource.setDriverClass(ClassUtils.forName(connectionDetails.getDriverClassName(),
            getClass().getClassLoader()));
    Awaitility.await().atMost(Duration.ofMinutes(1)).ignoreExceptions().untilAsserted(() -> {
      JdbcTemplate template = new JdbcTemplate(dataSource);
      String validationQuery = DatabaseDriver.ORACLE.getValidationQuery();
      assertThat(validationQuery).isNotNull();
      assertThat(template.queryForObject(validationQuery, String.class)).isEqualTo("Hello");
    });
  }

}
