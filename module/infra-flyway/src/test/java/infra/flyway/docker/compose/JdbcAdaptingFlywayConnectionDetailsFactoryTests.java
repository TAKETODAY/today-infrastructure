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

package infra.flyway.docker.compose;

import org.junit.jupiter.api.Test;

import infra.flyway.config.FlywayConnectionDetails;
import infra.jdbc.config.JdbcConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JdbcAdaptingFlywayConnectionDetailsFactoryTests {

  @Test
  void adaptsJdbcConnectionSettingsIncludingExplicitDriver() {
    JdbcConnectionDetails jdbc = mock();
    given(jdbc.getJdbcUrl()).willReturn("jdbc:postgresql://localhost:5432/app");
    given(jdbc.getDriverClassName()).willReturn("example.CustomDriver");
    given(jdbc.getUsername()).willReturn("migrator");
    given(jdbc.getPassword()).willReturn("test-password");

    FlywayConnectionDetails details = new JdbcAdaptingFlywayConnectionDetailsFactory().getConnectionDetails(jdbc);

    assertThat(details.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/app");
    assertThat(details.getDriverClassName()).isEqualTo("example.CustomDriver");
    assertThat(details.getUsername()).isEqualTo("migrator");
    assertThat(details.getPassword()).isEqualTo("test-password");
  }
}
