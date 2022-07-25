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

package cn.taketoday.jdbc;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.sql.Connection;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test to check if the autoCommit state has been reset upon close
 */
public class ConnectionTransactionTest {

  @Test
  public void beginTransaction() throws Exception {
    final DataSource dataSource = mock(DataSource.class);
    final Connection connectionMock = mock(Connection.class);

    // mocked behaviour
    when(dataSource.getConnection()).thenReturn(connectionMock);
    when(connectionMock.getAutoCommit()).thenReturn(true);
    when(connectionMock.isClosed()).thenReturn(false);

    final JdbcOperations sql2o = new JdbcOperations(dataSource);
    final JdbcConnection sql2oConnection = sql2o.beginTransaction();
    sql2oConnection.close();

    // Verifications
    verify(dataSource).getConnection();
    verify(connectionMock, atLeastOnce()).getAutoCommit();
    // called on beginTransaction
    verify(connectionMock, times(1)).setAutoCommit(ArgumentMatchers.eq(false));
    // called on closeConnection to reset autocommit state
    verify(connectionMock, times(1)).setAutoCommit(ArgumentMatchers.eq(true));
    verify(connectionMock, atLeastOnce()).setTransactionIsolation(ArgumentMatchers.anyInt());
    verify(connectionMock, times(1)).isClosed();
    verify(connectionMock, times(1)).close();
    verifyNoMoreInteractions(connectionMock, dataSource);
  }
}
