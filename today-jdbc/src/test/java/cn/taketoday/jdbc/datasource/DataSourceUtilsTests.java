/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/7/13 16:18
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
