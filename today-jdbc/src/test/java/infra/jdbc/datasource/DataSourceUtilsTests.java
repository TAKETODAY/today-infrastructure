/*
 * Copyright 2002-present the original author or authors.
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

package infra.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import javax.sql.DataSource;

import infra.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/13 16:18
 */
class DataSourceUtilsTests {

  @Test
  void testConnectionNotAcquiredExceptionIsPropagated() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(null);
    assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
            .isInstanceOf(CannotGetJdbcConnectionException.class)
            .hasMessageStartingWith("Failed to obtain JDBC Connection")
            .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void testConnectionSQLExceptionIsPropagated() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new SQLException("my dummy exception"));
    assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
            .isInstanceOf(CannotGetJdbcConnectionException.class)
            .hasMessageStartingWith("Failed to obtain JDBC Connection")
            .rootCause().isInstanceOf(SQLException.class)
            .hasMessage("my dummy exception");
  }

}
