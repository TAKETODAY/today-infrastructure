/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.datasource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.util.stream.Stream;

import infra.transaction.CannotCreateTransactionException;
import infra.transaction.NestedTransactionNotSupportedException;
import infra.transaction.TransactionSystemException;
import infra.transaction.TransactionUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JdbcTransactionObjectSupportTests {

  private final Connection connection = mock(Connection.class);

  private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);

  private final Savepoint savepoint = mock(Savepoint.class);

  private final ConnectionHolder holder = new ConnectionHolder(connection);

  private final JdbcTransactionObjectSupport transaction = new JdbcTransactionObjectSupport() { };

  @BeforeEach
  void configureTransaction() throws SQLException {
    given(connection.getMetaData()).willReturn(metadata);
    given(metadata.supportsSavepoints()).willReturn(true);
    transaction.setSavepointAllowed(true);
    transaction.setConnectionHolder(holder);
  }

  @Test
  void rejectsSavepointsWhenNestedTransactionsAreDisabledOrNoConnectionIsExposed() {
    transaction.setSavepointAllowed(false);
    assertThatThrownBy(transaction::createSavepoint).isInstanceOf(NestedTransactionNotSupportedException.class)
            .hasMessageContaining("does not allow nested transactions");

    transaction.setSavepointAllowed(true);
    transaction.setConnectionHolder(null);
    assertThatThrownBy(transaction::createSavepoint).isInstanceOf(TransactionUsageException.class)
            .hasMessageContaining("not exposing a JDBC transaction");
    verifyNoInteractions(connection);
  }

  @Test
  void rejectsSavepointsWhenDriverDoesNotSupportThem() throws SQLException {
    given(metadata.supportsSavepoints()).willReturn(false);

    assertThatThrownBy(transaction::createSavepoint).isInstanceOf(NestedTransactionNotSupportedException.class)
            .hasMessageContaining("not supported by your JDBC driver");
    verify(connection, never()).setSavepoint(anyString());
  }

  @Test
  void rejectsSavepointsWhenTransactionIsRollbackOnly() throws SQLException {
    holder.setRollbackOnly();

    assertThatThrownBy(transaction::createSavepoint).isInstanceOf(CannotCreateTransactionException.class)
            .hasMessageContaining("rollback-only");
    verify(connection, never()).setSavepoint(anyString());
  }

  @Test
  void wrapsFailureToCreateSavepoint() throws SQLException {
    var failure = new SQLException("cannot create savepoint");
    given(connection.setSavepoint("SAVEPOINT_1")).willThrow(failure);

    assertThatThrownBy(transaction::createSavepoint).isInstanceOf(CannotCreateTransactionException.class).hasCause(failure);
  }

  @Test
  void rollingBackToSavepointClearsRollbackOnlyOnlyOnSuccess() throws SQLException {
    var failure = new SQLException("cannot roll back to savepoint");
    willThrow(failure).willDoNothing().given(connection).rollback(savepoint);
    holder.setRollbackOnly();

    assertThatThrownBy(() -> transaction.rollbackToSavepoint(savepoint))
            .isInstanceOf(TransactionSystemException.class).hasCause(failure);
    assertThat(holder.isRollbackOnly()).isTrue();

    transaction.rollbackToSavepoint(savepoint);
    assertThat(holder.isRollbackOnly()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("ignorableReleaseFailures")
  void toleratesUnsupportedReleaseOrAnAlreadyReleasedSavepoint(SQLException failure) throws SQLException {
    willThrow(failure).given(connection).releaseSavepoint(savepoint);

    assertThatCode(() -> transaction.releaseSavepoint(savepoint)).doesNotThrowAnyException();
    verify(connection).releaseSavepoint(savepoint);
  }

  static Stream<SQLException> ignorableReleaseFailures() {
    return Stream.of(new SQLFeatureNotSupportedException(), new SQLException("already released", "3B001"),
            new SQLException("This operation is not supported."), new SQLException("Driver error 3B001"));
  }

  @ParameterizedTest
  @MethodSource("unexpectedReleaseFailures")
  void wrapsUnexpectedReleaseFailures(Exception failure) throws SQLException {
    willThrow(failure).given(connection).releaseSavepoint(savepoint);

    assertThatThrownBy(() -> transaction.releaseSavepoint(savepoint)).isInstanceOf(TransactionSystemException.class)
            .hasMessageContaining("explicitly release").hasCause(failure);
  }

  static Stream<Exception> unexpectedReleaseFailures() {
    return Stream.of(new SQLException(), new SQLException("connection lost", "08006"), new IllegalStateException("closed"));
  }

}
