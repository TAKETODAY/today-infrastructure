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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import infra.jdbc.datasource.DriverManagerDataSource;
import infra.transaction.support.TransactionTemplate;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:55
 */
class JdbcConnectionTests {

  @Test
  void closeIsIdempotentAndDoesNotAcquireAnUnusedConnection() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    JdbcConnection connection = new JdbcConnection(new RepositoryManager(dataSource), dataSource);
    connection.close();
    connection.close();
    verify(dataSource, never()).getConnection();
    assertThatThrownBy(() -> connection.createQuery("select 1"))
            .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
  }

  @Test
  void cannotStartTransactionAfterAcquiringConnection() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willReturn(mock(Connection.class));
    try (JdbcConnection connection = new RepositoryManager(dataSource).open()) {
      assertThatThrownBy(connection::beginTransaction)
              .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
    }
  }

  @Test
  void closeAttemptsAllStatementsRollbackAndConnectionRelease() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection root = mock(Connection.class);
    given(dataSource.getConnection()).willReturn(root);
    PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    TransactionStatus status = mock(TransactionStatus.class);
    given(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any())).willReturn(status);
    RepositoryManager repository = new RepositoryManager(dataSource, transactionManager);
    repository.setCatchResourceCloseErrors(true);
    JdbcConnection connection = repository.beginTransaction();
    Statement first = mock(Statement.class);
    Statement second = mock(Statement.class);
    willThrow(new SQLException("first close failed")).given(first).close();
    willThrow(new SQLException("second close failed")).given(second).close();
    connection.registerStatement(first);
    connection.registerStatement(second);
    assertThatThrownBy(connection::close).isInstanceOf(infra.dao.DataAccessException.class)
            .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(1));
    verify(first).close();
    verify(second).close();
    verify(transactionManager).rollback(status);
    verify(root).close();
    connection.close();
  }

  @Test
  void rollbackFailureIsSuppressedOnOriginalQueryFailure() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willReturn(mock(Connection.class));
    PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    TransactionStatus status = mock(TransactionStatus.class);
    given(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any())).willReturn(status);
    RuntimeException rollbackFailure = new IllegalStateException("rollback failed");
    willThrow(rollbackFailure).given(transactionManager).rollback(status);
    JdbcConnection connection = new RepositoryManager(dataSource, transactionManager).beginTransaction();
    SQLException failure = new SQLException("SQL failed");
    connection.onException(failure);
    assertThat(failure.getSuppressed()).containsExactly(rollbackFailure);
    given(status.isCompleted()).willReturn(true);
    connection.close();
  }

  @Test
  void managedTransactionCanCommitRollbackAndRollBackOnClose() {
    RepositoryManager repository = new RepositoryManager(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    repository.createQuery("create table native_transaction (id integer primary key)").executeUpdate();

    try (JdbcConnection connection = repository.beginTransaction()) {
      connection.createQuery("insert into native_transaction values (1)").executeUpdate();
      connection.commit(false);
    }
    try (JdbcConnection connection = repository.beginTransaction()) {
      connection.createQuery("insert into native_transaction values (2)").executeUpdate();
      connection.rollback(false);
    }
    try (JdbcConnection connection = repository.beginTransaction()) {
      connection.createQuery("insert into native_transaction values (3)").executeUpdate();
    }

    assertThat(repository.createQuery("select id from native_transaction").fetch(Integer.class))
            .containsExactly(1);
  }

  @Test
  void managedTransactionRollsBackOnExecutionFailure() throws SQLException {
    RepositoryManager repository = new RepositoryManager(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    repository.createQuery("create table native_transaction (id integer primary key)").executeUpdate();
    try (JdbcConnection connection = repository.beginTransaction()) {
      connection.createQuery("insert into native_transaction values (1)").executeUpdate();
      assertThatThrownBy(() -> connection.createQuery("insert into native_transaction values (1)").executeUpdate())
              .isInstanceOf(infra.dao.DataAccessException.class);
      assertThatThrownBy(() -> connection.commit(false))
              .isInstanceOf(infra.transaction.IllegalTransactionStateException.class);
    }
    assertThat(repository.createQuery("select count(*) from native_transaction").fetchFirst(Integer.class)).isZero();
  }

  @Test
  void participatingConnectionDoesNotCompleteOuterTransaction() {
    RepositoryManager repository = new RepositoryManager(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    repository.createQuery("create table outer_transaction (id integer primary key)").executeUpdate();
    new TransactionTemplate(repository.getTransactionManager()).executeWithoutResult(status -> {
      try (JdbcConnection connection = repository.open()) {
        connection.createQuery("insert into outer_transaction values (1)").executeUpdate();
        assertThatThrownBy(() -> connection.rollback(false))
                .isInstanceOf(infra.transaction.IllegalTransactionStateException.class);
        assertThat(connection.createQuery("select count(*) from outer_transaction").fetchFirst(Integer.class))
                .isEqualTo(1);
        assertThatThrownBy(() -> connection.commit(false))
                .isInstanceOf(infra.transaction.IllegalTransactionStateException.class);
      }
      status.setRollbackOnly();
    });
    assertThat(repository.createQuery("select count(*) from outer_transaction").fetchFirst(Integer.class)).isZero();
  }

  @Test
  void autoCommitConnectionsDoNotInvokeNativeTransactionCompletion() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection root = mock(Connection.class);
    given(dataSource.getConnection()).willReturn(root);
    given(root.getAutoCommit()).willReturn(true);
    try (JdbcConnection connection = new RepositoryManager(dataSource).open()) {
      assertThatThrownBy(() -> connection.commit(false))
              .isInstanceOf(infra.transaction.IllegalTransactionStateException.class);
      assertThatThrownBy(() -> connection.rollback(false))
              .isInstanceOf(infra.transaction.IllegalTransactionStateException.class);
    }
    verify(root, never()).commit();
    verify(root, never()).rollback();
  }

  @Test
  void completedTransactionIsClearedWhenCommitFails() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection root = mock(Connection.class);
    given(dataSource.getConnection()).willReturn(root);
    SQLException failure = new SQLException("completion failed");
    willThrow(failure).given(root).commit();
    willThrow(failure).given(root).rollback();
    JdbcConnection connection = new RepositoryManager(dataSource).beginTransaction();
    assertThatThrownBy(() -> connection.commit(false))
            .isInstanceOf(infra.transaction.TransactionException.class).hasCause(failure);
    assertThatThrownBy(() -> connection.rollback(false))
            .isInstanceOf(infra.transaction.IllegalTransactionStateException.class);
    connection.setRollbackOnClose(false);
    connection.close();
  }

  @Test
  void shouldCreateJdbcConnectionWithoutAutoClose() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection).isNotNull();
    assertThat(connection.autoClose).isFalse();
  }

  @Test
  void shouldGetManager() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection.getManager()).isEqualTo(manager);
  }

  @Test
  void shouldSetAndGetRollbackOnException() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection.isRollbackOnException()).isTrue();

    connection.setRollbackOnException(false);
    assertThat(connection.isRollbackOnException()).isFalse();

    connection.setRollbackOnException(true);
    assertThat(connection.isRollbackOnException()).isTrue();
  }

  @Test
  void shouldSetAndGetRollbackOnClose() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection.isRollbackOnClose()).isTrue();

    connection.setRollbackOnClose(false);
    assertThat(connection.isRollbackOnClose()).isFalse();

    connection.setRollbackOnClose(true);
    assertThat(connection.isRollbackOnClose()).isTrue();
  }

  @Test
  void shouldHandleOnExceptionWithRollback() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);
    connection.setRollbackOnException(true);

    connection.onException(new SQLException("query failed"));

    // Should not throw exception
    assertThat(true).isTrue();
  }

}
