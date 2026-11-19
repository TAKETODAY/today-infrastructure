/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.docker.compose;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.docker.compose.core.ConnectionPorts;
import infra.docker.compose.core.RunningService;
import infra.docker.compose.service.connection.DockerComposeConnectionSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JdbcDockerComposeConnectionDetailsFactoryTests {

  @Test
  void mysqlUsesMappedPortAndApplicationCredentials() {
    var source = source(3306, Map.of("MYSQL_DATABASE", "sales", "MYSQL_USER", "app", "MYSQL_PASSWORD", "test"), Map.of());

    var details = new MySqlJdbcDockerComposeConnectionDetailsFactory().getDockerComposeConnectionDetails(source);

    assertThat(details.getJdbcUrl()).isEqualTo("jdbc:mysql://db.example:25432/sales");
    assertThat(details.getUsername()).isEqualTo("app");
    assertThat(details.getPassword()).isEqualTo("test");
  }

  @Test
  void mariaDbUsesMariaDbCredentialsAndJdbcParameters() {
    var source = source(3306,
            Map.of("MARIADB_DATABASE", "sales", "MARIADB_USER", "app", "MARIADB_PASSWORD", "test", "MYSQL_USER", "legacy"),
            Map.of("infra.jdbc.parameters", "connectTimeout=250"));

    var details = new MariaDbJdbcDockerComposeConnectionDetailsFactory().getDockerComposeConnectionDetails(source);

    assertThat(details.getJdbcUrl()).isEqualTo("jdbc:mariadb://db.example:25432/sales?connectTimeout=250");
    assertThat(details.getUsername()).isEqualTo("app");
    assertThat(details.getPassword()).isEqualTo("test");
  }

  @Test
  void clickHouseUsesItsHttpPortAndDefaultDatabase() {
    var source = source(8123, Map.of("CLICKHOUSE_PASSWORD", "test"), Map.of());

    var details = new ClickHouseJdbcDockerComposeConnectionDetailsFactory().getDockerComposeConnectionDetails(source);

    assertThat(details.getJdbcUrl()).isEqualTo("jdbc:clickhouse://db.example:25432/default");
    assertThat(details.getUsername()).isEqualTo("default");
    assertThat(details.getPassword()).isEqualTo("test");
  }

  @Test
  void oracleFactoriesUseEditionSpecificDefaults() {
    var source = source(1521, Map.of("ORACLE_PASSWORD", "test"), Map.of());

    var free = new OracleFreeJdbcDockerComposeConnectionDetailsFactory().getDockerComposeConnectionDetails(source);
    var xe = new OracleXeJdbcDockerComposeConnectionDetailsFactory().getDockerComposeConnectionDetails(source);

    assertThat(free.getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@db.example:25432/freepdb1");
    assertThat(xe.getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@db.example:25432/xepdb1");
    assertThat(xe.getUsername()).isEqualTo("system");
    assertThat(xe.getPassword()).isEqualTo("test");
  }

  @Test
  void oracleAppUserCanOverrideDatabaseAndAppendJdbcParameters() {
    var source = source(1521, Map.of("ORACLE_DATABASE", "sales", "APP_USER", "app", "APP_USER_PASSWORD", "test"),
            Map.of("infra.jdbc.parameters", "oracle.net.CONNECT_TIMEOUT=250"));

    var details = new OracleFreeJdbcDockerComposeConnectionDetailsFactory().getDockerComposeConnectionDetails(source);

    assertThat(details.getJdbcUrl()).isEqualTo("jdbc:oracle:thin:@db.example:25432/sales?oracle.net.CONNECT_TIMEOUT=250");
    assertThat(details.getUsername()).isEqualTo("app");
    assertThat(details.getPassword()).isEqualTo("test");
  }

  private static DockerComposeConnectionSource source(int containerPort, Map<String, String> env, Map<String, String> labels) {
    var source = mock(DockerComposeConnectionSource.class);
    var service = mock(RunningService.class);
    var ports = mock(ConnectionPorts.class);
    given(source.getRunningService()).willReturn(service);
    given(service.host()).willReturn("db.example");
    given(service.ports()).willReturn(ports);
    given(ports.get(containerPort)).willReturn(25432);
    given(service.env()).willReturn(env);
    given(service.labels()).willReturn(labels);
    return source;
  }

}
