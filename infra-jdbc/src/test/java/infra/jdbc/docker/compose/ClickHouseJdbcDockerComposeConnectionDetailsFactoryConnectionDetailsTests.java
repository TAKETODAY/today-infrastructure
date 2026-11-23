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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.docker.compose.core.ConnectionPorts;
import infra.docker.compose.core.RunningService;
import infra.jdbc.config.JdbcConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for
 * {@link ClickHouseJdbcDockerComposeConnectionDetailsFactory.ClickhouseJdbcDockerComposeConnectionDetails}.
 */
class ClickHouseJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests {

  private final RunningService service = mock(RunningService.class);

  private final LinkedHashMap<String, String> labels = new LinkedHashMap<>();

  ClickHouseJdbcDockerComposeConnectionDetailsFactoryConnectionDetailsTests() {
    given(this.service.env()).willReturn(
            Map.of("CLICKHOUSE_USER", "user", "CLICKHOUSE_PASSWORD", "password", "CLICKHOUSE_DB", "database"));
    given(this.service.labels()).willReturn(this.labels);
    ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
    given(this.service.ports()).willReturn(connectionPorts);
    given(this.service.host()).willReturn("localhost");
    given(connectionPorts.get(8123)).willReturn(30001);
  }

  @Test
  void createConnectionDetails() {
    JdbcConnectionDetails connectionDetails = getConnectionDetails();
    assertConnectionDetails(connectionDetails);
    assertThat(connectionDetails.getJdbcUrl()).endsWith("/database");
  }

  @ParameterizedTest
  @ValueSource(strings = { "my?db", "my/db", "my%db", "my db" })
  void createConnectionDetailsWithDatabaseNameRequiringEncoding(String database) {
    given(this.service.env()).willReturn(
            Map.of("CLICKHOUSE_USER", "user", "CLICKHOUSE_PASSWORD", "password", "CLICKHOUSE_DB", database));
    JdbcConnectionDetails connectionDetails = getConnectionDetails();
    // The driver leaves a literal '+' untouched, so spaces must be encoded as %20.
    assertThat(connectionDetails.getJdbcUrl()).endsWith("/" + percentEncode(database));
  }

  @Test
  void createConnectionDetailsWithLabels() {
    this.labels.put("infra.jdbc.parameters", "connect_timeout=30");
    JdbcConnectionDetails connectionDetails = getConnectionDetails();
    assertConnectionDetails(connectionDetails);
    assertThat(connectionDetails.getJdbcUrl()).endsWith("?connect_timeout=30");
  }

  private void assertConnectionDetails(JdbcConnectionDetails connectionDetails) {
    assertThat(connectionDetails.getUsername()).isEqualTo("user");
    assertThat(connectionDetails.getPassword()).isEqualTo("password");
    assertThat(connectionDetails.getJdbcUrl()).startsWith("jdbc:clickhouse://localhost:30001/database");
  }

  private JdbcConnectionDetails getConnectionDetails() {
    return new ClickHouseJdbcDockerComposeConnectionDetailsFactory.ClickhouseJdbcDockerComposeConnectionDetails(
            this.service);
  }

  static String percentEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

}
