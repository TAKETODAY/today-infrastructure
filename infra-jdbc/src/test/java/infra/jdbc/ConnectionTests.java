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

package infra.jdbc;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.assertThat;
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
class ConnectionTests {

  @Test
  void generatedKeysAreRequestedOnlyWhenExplicitlyEnabled() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    String sql = "insert into items(name) values ('item')";
    when(jdbcConnection.prepareStatement(sql)).thenReturn(statement);
    when(jdbcConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)).thenReturn(statement);

    RepositoryManager repository = new RepositoryManager(dataSource);
    try (JdbcConnection connection = repository.open()) {
      Query query = connection.createQuery(sql);
      NamedQuery namedQuery = connection.createNamedQuery(sql);
      assertThat(query.isReturnGeneratedKeys()).isFalse();
      assertThat(namedQuery.isReturnGeneratedKeys()).isFalse();
      query.executeUpdate();
      namedQuery.executeUpdate();
      verify(statement, never()).getGeneratedKeys();

      Query withKeys = connection.createQuery(sql, true);
      NamedQuery namedWithKeys = connection.createNamedQuery(sql, true);
      assertThat(withKeys.isReturnGeneratedKeys()).isTrue();
      assertThat(namedWithKeys.isReturnGeneratedKeys()).isTrue();
      withKeys.buildStatement();
      namedWithKeys.buildStatement();
      verify(jdbcConnection, times(2)).prepareStatement(sql);
      verify(jdbcConnection, times(2)).prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
  }

  @Test
  void createQueryWithParams() throws Throwable {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    when(jdbcConnection.isClosed()).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(jdbcConnection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(ps);

    RepositoryManager operations = new RepositoryManager(dataSource);

    JdbcConnection cn = new JdbcConnection(operations, operations.getDataSource(), false);
    cn.createNamedQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildStatement();

    verify(dataSource, times(1)).getConnection();
    verify(jdbcConnection).isClosed();
    verify(jdbcConnection, times(1)).prepareStatement("select ? name, ? age");
    verify(ps, times(1)).setString(1, "Dmitry Alexandrov");
    verify(ps, times(1)).setInt(2, 35);
    // check statement still alive
    verify(ps, never()).close();

  }

  @SuppressWarnings("serial")
  public class MyException extends RuntimeException {
  }

  @Test
  void createQueryWithParamsThrowingException() throws Throwable {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    when(jdbcConnection.isClosed()).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    PreparedStatement ps = mock(PreparedStatement.class);
    doThrow(MyException.class).when(ps).setInt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    when(jdbcConnection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(ps);

    RepositoryManager manager = new RepositoryManager(dataSource);
    try (JdbcConnection cn = manager.open()) {
      cn.createNamedQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildStatement();
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
