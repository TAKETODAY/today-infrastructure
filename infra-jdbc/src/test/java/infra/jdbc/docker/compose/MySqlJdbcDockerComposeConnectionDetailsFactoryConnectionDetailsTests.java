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

import com.mysql.cj.conf.ConnectionUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import infra.docker.compose.core.ConnectionPorts;
import infra.docker.compose.core.RunningService;
import infra.jdbc.config.JdbcConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for
 * {@link MySqlJdbcDockerComposeConnectionDetailsFactory.MySqlJdbcDockerComposeConnectionDetails}.
 */
class MySqlJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

  private final RunningService service = mock(RunningService.class);

  private final LinkedHashMap<String, String> labels = new LinkedHashMap<>();

  MySqlJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
    given(this.service.env())
            .willReturn(Map.of("MYSQL_USER", "user", "MYSQL_PASSWORD", "password", "MYSQL_DATABASE", "database"));
    given(this.service.labels()).willReturn(this.labels);
    ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
    given(this.service.ports()).willReturn(connectionPorts);
    given(this.service.host()).willReturn("localhost");
    given(connectionPorts.get(3306)).willReturn(30001);
  }

  @Test
  void createConnectionDetails() {
    JdbcConnectionDetails connectionDetails = getConnectionDetails();
    assertConnectionDetails(connectionDetails);
    assertThat(connectionDetails.getJdbcUrl()).endsWith("/database");
  }

  @ParameterizedTest
  @ValueSource(strings = { "my+db", "my?db", "my/db", "my%db", "my db", "my数据库" })
  void createConnectionDetailsWithDatabaseNameRequiringEncoding(String database) {
    given(this.service.env())
            .willReturn(Map.of("MYSQL_USER", "user", "MYSQL_PASSWORD", "password", "MYSQL_DATABASE", database));
    JdbcConnectionDetails connectionDetails = getConnectionDetails();
    assertThat(ConnectionUrl.getConnectionUrlInstance(connectionDetails.getJdbcUrl(), new Properties()).getDatabase())
            .isEqualTo(database);
  }

  @Test
  void createConnectionDetailsWithLabels() {
    this.labels.put("infra.jdbc.parameters", "connectTimeout=30");
    JdbcConnectionDetails connectionDetails = getConnectionDetails();
    assertConnectionDetails(connectionDetails);
    assertThat(connectionDetails.getJdbcUrl()).endsWith("?connectTimeout=30");
  }

  private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
    assertThat(connectionDetails.getUsername()).isEqualTo("user");
    assertThat(connectionDetails.getPassword()).isEqualTo("password");
    assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:mysql://localhost:30001/database");
  }

  private JdbcConnectionDetails getConnectionDetails() {
    return new MySqlJdbcDockerComposeConnectionDetailsFactory.MySqlJdbcDockerComposeConnectionDetails(this.service);
  }

}
