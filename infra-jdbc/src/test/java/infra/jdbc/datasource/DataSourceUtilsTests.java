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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;

import javax.sql.DataSource;

import infra.jdbc.CannotGetJdbcConnectionException;
import infra.transaction.support.DefaultTransactionDefinition;
import infra.transaction.support.SynchronizationInfo;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

  @Test
  void reacquiresReleasedConnectionAfterSynchronizationResumes() throws SQLException {
    DataSource target = mock();
    var dataSource = new DelegatingDataSource(new DelegatingDataSource(target));
    Connection first = mock();
    Connection second = mock();
    given(target.getConnection()).willReturn(first, second);

    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(first);
      assertThat(DataSourceUtils.isConnectionTransactional(first, dataSource)).isTrue();
      assertThat(DataSourceUtils.isConnectionTransactional(second, dataSource)).isFalse();
      DataSourceUtils.releaseConnection(first, dataSource);
      verify(first, never()).close();

      var synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
      assertThat(synchronization.getOrder()).isEqualTo(DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 2);
      synchronization.suspend();
      assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
      verify(first).close();
      synchronization.resume();
      assertThat(DataSourceUtils.doGetConnection(dataSource)).isSameAs(second);
      DataSourceUtils.releaseConnection(second, dataSource);

      synchronization.beforeCompletion();
      verify(second).close();
      synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
      verify(second).close();
    }
    finally {
      TransactionSynchronizationManager.unbindResourceIfPossible(dataSource);
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void keepsBorrowedConnectionUntilAfterCompletion() throws SQLException {
    DataSource dataSource = mock();
    Connection connection = mock();
    var holder = new ConnectionHolder(connection);
    holder.setConnection(null);
    given(dataSource.getConnection()).willReturn(connection);
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.bindResource(dataSource, holder);
    try {
      assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(connection);
      assertThat(TransactionSynchronizationManager.<ConnectionHolder>getResource(dataSource)).isSameAs(holder);
      var synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
      synchronization.suspend();
      synchronization.resume();
      assertThat(holder.getConnection()).isSameAs(connection);

      synchronization.beforeCompletion();
      verify(connection, never()).close();
      synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

      verify(connection).close();
      assertThat(holder.hasConnection()).isFalse();
      assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    }
    finally {
      TransactionSynchronizationManager.unbindResourceIfPossible(dataSource);
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void releasesConnectionWhenSynchronizationRegistrationFails() throws SQLException {
    DataSource dataSource = mock();
    Connection connection = mock();
    SynchronizationInfo info = mock();
    var failure = new IllegalStateException("registration failed");
    given(info.isSynchronizationActive()).willReturn(true);
    given(dataSource.getConnection()).willReturn(connection);
    willThrow(failure).given(info).registerSynchronization(any());

    assertThatThrownBy(() -> DataSourceUtils.doGetConnection(info, dataSource)).isSameAs(failure);
    verify(connection).close();
  }

  @Test
  void toleratesUnsupportedReadOnlyHintButPropagatesNestedTimeout() throws SQLException {
    Connection connection = mock();
    var unsupported = new SQLException("read-only not supported");
    var timeout = new SQLException("read-only timed out", new SQLTimeoutException("timeout"));
    willThrow(unsupported, timeout).given(connection).setReadOnly(true);
    given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);

    assertThat(DataSourceUtils.prepareConnectionForTransaction(connection, Connection.TRANSACTION_SERIALIZABLE, true))
            .isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
    DataSourceUtils.resetConnectionAfterTransaction(connection, Connection.TRANSACTION_READ_COMMITTED, true);
    assertThatThrownBy(() -> DataSourceUtils.setReadOnlyIfPossible(connection)).isSameAs(timeout);

    var order = inOrder(connection);
    order.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    order.verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    order.verify(connection).setReadOnly(false);
  }

  @Test
  void leavesStatementTimeoutUnsetUnlessExplicitlyConfigured() throws SQLException {
    Statement statement = mock();
    DataSourceUtils.applyTransactionTimeout(statement, null);
    verify(statement, never()).setQueryTimeout(anyInt());

    DataSourceUtils.applyTimeout(statement, null, 0);
    verify(statement).setQueryTimeout(0);
  }

  @Test
  void transactionTimeoutOverridesStatementTimeout() throws SQLException {
    DataSource dataSource = mock();
    ConnectionHolder holder = mock();
    Statement statement = mock();
    given(holder.hasTimeout()).willReturn(true);
    given(holder.getTimeToLiveInSeconds()).willReturn(7);
    TransactionSynchronizationManager.bindResource(dataSource, holder);
    try {
      DataSourceUtils.applyTimeout(statement, dataSource, 30);
      verify(statement).setQueryTimeout(7);
      verify(statement, never()).setQueryTimeout(30);
    }
    finally {
      TransactionSynchronizationManager.unbindResource(dataSource);
    }
  }

  @Test
  void leavesConnectionUnchangedWhenNoTransactionDefinitionIsSupplied() throws SQLException {
    Connection connection = mock();

    assertThat(DataSourceUtils.prepareConnectionForTransaction(connection, null)).isNull();
    DataSourceUtils.resetConnectionAfterTransaction(connection, null, false);

    verifyNoInteractions(connection);
    assertThat(DataSourceUtils.isConnectionTransactional(connection, null)).isFalse();
  }

  @Test
  void appliesReadOnlyWithoutResettingAnAlreadyMatchingIsolationLevel() throws SQLException {
    Connection connection = mock();
    var definition = new DefaultTransactionDefinition();
    definition.setReadOnly(true);
    definition.setIsolationLevel(Connection.TRANSACTION_SERIALIZABLE);
    given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_SERIALIZABLE);

    Integer previous = DataSourceUtils.prepareConnectionForTransaction(connection, definition);
    assertThat(previous).isNull();
    DataSourceUtils.resetConnectionAfterTransaction(connection, previous, true);

    var order = inOrder(connection);
    order.verify(connection).setReadOnly(true);
    order.verify(connection).getTransactionIsolation();
    order.verify(connection).setReadOnly(false);
    verify(connection, never()).setTransactionIsolation(anyInt());
  }

  @Test
  void cleanupSuppressesDriverFailuresButExplicitReleasePropagatesThem() throws SQLException {
    Connection connection = mock();
    var closeFailure = new IllegalStateException("broken connection wrapper");
    willThrow(new SQLException("cannot reset read-only mode")).given(connection).setReadOnly(false);
    willThrow(closeFailure).given(connection).close();

    assertThatCode(() -> DataSourceUtils.resetConnectionAfterTransaction(connection, null, true)).doesNotThrowAnyException();
    assertThatCode(() -> DataSourceUtils.releaseConnection(connection, null)).doesNotThrowAnyException();
    assertThatThrownBy(() -> DataSourceUtils.doReleaseConnection(connection, null)).isSameAs(closeFailure);
  }

  @Test
  void closesNonTransactionalConnectionButPreservesSmartDataSourceConnection() throws SQLException {
    Connection connection = mock();
    DataSource dataSource = mock(SmartDataSource.class);
    given(((SmartDataSource) dataSource).shouldClose(connection)).willReturn(false);

    DataSourceUtils.doReleaseConnection(connection, dataSource);
    verify(connection, never()).close();

    given(((SmartDataSource) dataSource).shouldClose(connection)).willReturn(true);
    DataSourceUtils.doReleaseConnection(connection, dataSource);
    verify(connection).close();
  }

  @Test
  void unwrapsNestedConnectionProxiesAndStopsAtSelfReference() {
    Connection target = mock();
    ConnectionProxy outer = mock();
    ConnectionProxy inner = mock();
    given(outer.getTargetConnection()).willReturn(inner);
    given(inner.getTargetConnection()).willReturn(target, inner);

    assertThat(DataSourceUtils.getTargetConnection(outer)).isSameAs(target);
    assertThat(DataSourceUtils.getTargetConnection(outer)).isSameAs(inner);
  }

}
