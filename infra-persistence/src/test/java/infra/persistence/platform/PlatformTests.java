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

package infra.persistence.platform;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import infra.jdbc.config.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class PlatformTests {

  @Test
  void resolvesMySQLByProductName() {
    assertThat(Platform.forDatabaseProductName("MySQL")).isInstanceOf(MySQLPlatform.class);
    assertThat(Platform.forDatabaseProductName("MariaDB")).isInstanceOf(MySQLPlatform.class);
  }

  @Test
  void fallsBackToGenericForUnsupportedProduct() {
    assertThat(Platform.forDatabaseProductName("H2")).isInstanceOf(GenericPlatform.class);
    assertThat(Platform.forDatabaseProductName("PostgreSQL")).isInstanceOf(GenericPlatform.class);
    assertThat(Platform.forDatabaseProductName(null)).isInstanceOf(GenericPlatform.class);
  }

  @Test
  void resolvesByDriver() {
    assertThat(Platform.forDriver(DatabaseDriver.MYSQL)).isInstanceOf(MySQLPlatform.class);
    assertThat(Platform.forDriver(DatabaseDriver.MARIADB)).isInstanceOf(MySQLPlatform.class);
    assertThat(Platform.forDriver(DatabaseDriver.POSTGRESQL)).isInstanceOf(GenericPlatform.class);
    assertThat(Platform.forDriver(DatabaseDriver.UNKNOWN)).isInstanceOf(GenericPlatform.class);
  }

  @Test
  void resolvesFromDataSourceMetadata() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(metaData);
    given(metaData.getDatabaseProductName()).willReturn("MySQL");

    assertThat(Platform.forDataSource(dataSource)).isInstanceOf(MySQLPlatform.class);
  }

  @Test
  void fallsBackToGenericWhenMetadataUnavailable() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willThrow(new SQLException("no connection"));

    assertThat(Platform.forDataSource(dataSource)).isInstanceOf(GenericPlatform.class);
  }

  @Test
  void handlesNullDataSource() {
    assertThat(Platform.forDataSource(null)).isInstanceOf(GenericPlatform.class);
  }

}
