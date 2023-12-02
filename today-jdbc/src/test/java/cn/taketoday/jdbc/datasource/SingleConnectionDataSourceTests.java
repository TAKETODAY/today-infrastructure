/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/2 21:13
 */
class SingleConnectionDataSourceTests {

  private final Connection connection = mock();

  @Test
  void plainConnection() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, false);

    ds.getConnection().close();
    verify(connection, times(1)).close();
  }

  @Test
  void withAutoCloseable() throws Exception {
    try (SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, false)) {
      ds.getConnection();
    }

    verify(connection, times(1)).close();
  }

  @Test
  void withSuppressClose() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);

    ds.getConnection().close();
    verify(connection, never()).close();

    ds.destroy();
    verify(connection, times(1)).close();

    given(connection.isClosed()).willReturn(true);
    assertThatExceptionOfType(SQLException.class).isThrownBy(ds::getConnection);
  }

  @Test
  void withRollbackBeforeClose() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);
    ds.setRollbackBeforeClose(true);

    ds.destroy();
    verify(connection, times(1)).rollback();
    verify(connection, times(1)).close();
  }

  @Test
  void withEnforcedAutoCommit() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource() {
      @Override
      protected Connection getConnectionFromDriverManager(String url, Properties props) {
        return connection;
      }
    };
    ds.setUrl("url");
    ds.setAutoCommit(true);

    ds.getConnection();
    verify(connection, times(1)).setAutoCommit(true);

    ds.destroy();
    verify(connection, times(1)).close();
  }

}