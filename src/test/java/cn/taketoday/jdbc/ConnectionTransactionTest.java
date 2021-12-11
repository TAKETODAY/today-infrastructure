package cn.taketoday.jdbc;

import org.junit.jupiter.api.Test;

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
    verify(connectionMock, times(1)).setAutoCommit(eq(false));
    // called on closeConnection to reset autocommit state
    verify(connectionMock, times(1)).setAutoCommit(eq(true));
    verify(connectionMock, atLeastOnce()).setTransactionIsolation(anyInt());
    verify(connectionMock, times(1)).isClosed();
    verify(connectionMock, times(1)).close();
    verifyNoMoreInteractions(connectionMock, dataSource);
  }
}
