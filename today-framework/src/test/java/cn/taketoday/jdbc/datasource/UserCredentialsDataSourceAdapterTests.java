/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 */
public class UserCredentialsDataSourceAdapterTests {

  @Test
  public void testStaticCredentials() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    given(dataSource.getConnection("user", "pw")).willReturn(connection);

    UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
    adapter.setTargetDataSource(dataSource);
    adapter.setUsername("user");
    adapter.setPassword("pw");
    assertThat(adapter.getConnection()).isEqualTo(connection);
  }

  @Test
  public void testNoCredentials() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    given(dataSource.getConnection()).willReturn(connection);
    UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
    adapter.setTargetDataSource(dataSource);
    assertThat(adapter.getConnection()).isEqualTo(connection);
  }

  @Test
  public void testThreadBoundCredentials() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    given(dataSource.getConnection("user", "pw")).willReturn(connection);

    UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
    adapter.setTargetDataSource(dataSource);

    adapter.setCredentialsForCurrentThread("user", "pw");
    try {
      assertThat(adapter.getConnection()).isEqualTo(connection);
    }
    finally {
      adapter.removeCredentialsFromCurrentThread();
    }
  }

}
