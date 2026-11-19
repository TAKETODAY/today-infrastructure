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

package infra.jdbc.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import infra.app.health.contributor.Status;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DataSourceHealthIndicatorTests {

  @Test
  void reportsUnknownDatabaseBeforeInitializationWithoutDataSource() {
    var indicator = new DataSourceHealthIndicator();
    var health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("database", "unknown");
    assertThatThrownBy(indicator::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("DataSource");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void usesDriverValidationWhenThereIsNoQuery(boolean valid) throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(metadata);
    given(metadata.getDatabaseProductName()).willReturn("ExampleDB");
    given(connection.isValid(0)).willReturn(valid);
    var indicator = new DataSourceHealthIndicator(dataSource);

    indicator.afterPropertiesSet();
    var health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(valid ? Status.UP : Status.DOWN);
    assertThat(health.getDetails()).containsEntry("database", "ExampleDB")
            .containsEntry("validationQuery", "isValid()");
  }

  @Test
  void runsConfiguredValidationQuery() {
    var source = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
    try {
      var indicator = new DataSourceHealthIndicator();
      indicator.setDataSource(source);
      indicator.setQuery("SELECT 42");

      assertThat(indicator.getQuery()).isEqualTo("SELECT 42");
      var health = indicator.health();
      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("database", "H2")
              .containsEntry("validationQuery", "SELECT 42").containsEntry("result", 42);
    }
    finally {
      source.shutdown();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "SELECT 1, 2", "SELECT NULL", "SELECT 1 WHERE 1 = 0", "VALUES (1), (2)" })
  void rejectsQueriesThatDoNotReturnOneNonNullValue(String query) {
    var source = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
    try {
      var health = new DataSourceHealthIndicator(source, query).health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsEntry("validationQuery", query).containsKey("error");
    }
    finally {
      source.shutdown();
    }
  }

}
