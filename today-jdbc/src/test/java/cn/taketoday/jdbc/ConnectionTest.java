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
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: dimzon Date: 4/29/14 Time: 10:05 PM
 */
public class ConnectionTest {
  @Test
  public void test_createQueryWithParams() throws Throwable {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    when(jdbcConnection.isClosed()).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(jdbcConnection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(ps);

    RepositoryManager operations = new RepositoryManager(dataSource);

    operations.setGeneratedKeys(false);
    JdbcConnection cn = new JdbcConnection(operations, operations.getDataSource(), false);
    cn.createQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildPreparedStatement();

    verify(dataSource, times(1)).getConnection();
    verify(jdbcConnection).isClosed();
    verify(jdbcConnection, times(1)).prepareStatement("select ? name, ? age");
    verify(ps, times(1)).setString(1, "Dmitry Alexandrov");
    verify(ps, times(1)).setInt(2, 35);
    // check statement still alive
    verify(ps, never()).close();

  }

  public class MyException extends RuntimeException { }

  @Test
  public void test_createQueryWithParamsThrowingException() throws Throwable {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    when(jdbcConnection.isClosed()).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    PreparedStatement ps = mock(PreparedStatement.class);
    doThrow(MyException.class).when(ps).setInt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    when(jdbcConnection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(ps);

    RepositoryManager sql2o = new RepositoryManager(dataSource, false);
    try (JdbcConnection cn = sql2o.open()) {
      cn.createQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildPreparedStatement();
      fail("exception not thrown");
    }
    catch (MyException ex) {
      // as designed
    }
    verify(dataSource, times(1)).getConnection();
    verify(jdbcConnection, atLeastOnce()).isClosed();
    verify(jdbcConnection, times(1)).prepareStatement("select ? name, ? age");
    verify(ps, times(1)).setInt(2, 35);
    // check statement was closed
    verify(ps, times(1)).close();
  }
}
