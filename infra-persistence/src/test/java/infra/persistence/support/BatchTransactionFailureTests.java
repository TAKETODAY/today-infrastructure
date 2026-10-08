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

package infra.persistence.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.JdbcConnection;
import infra.jdbc.PersistenceException;
import infra.jdbc.RepositoryManager;
import infra.persistence.annotation.Table;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BatchTransactionFailureTests {

  final RepositoryManager repository = mock(RepositoryManager.class);
  final JdbcConnection transaction = mock(JdbcConnection.class);
  final TransactionStatus status = mock(TransactionStatus.class);
  final PreparedStatement statement = mock(PreparedStatement.class);

  DefaultEntityManager manager;

  @BeforeEach
  void setup() throws SQLException {
    given(repository.getDataSource()).willReturn(mock(DataSource.class));
    given(repository.beginTransaction(any(TransactionDefinition.class))).willReturn(transaction);
    given(transaction.getTransaction()).willReturn(status);
    Connection connection = mock(Connection.class);
    given(transaction.getJdbcConnection()).willReturn(connection);
    given(connection.prepareStatement(anyString())).willReturn(statement);
    given(statement.executeBatch()).willReturn(new int[] { 1 });
    org.mockito.BDDMockito.willCallRealMethod().given(repository)
            .closeResource(any(AutoCloseable.class), anyString(), isNull());
    org.mockito.BDDMockito.willCallRealMethod().given(repository)
            .closeResource(any(AutoCloseable.class), anyString(), any(Throwable.class));
    manager = new DefaultEntityManager(repository) {
      @Override
      protected PreparedStatement prepareStatement(Connection connection, String sql, boolean generatedKeys) {
        return statement;
      }
    };
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void rollbackFailureDoesNotReplaceOriginalFailure(boolean update) throws SQLException {
    var failure = new InvalidDataAccessApiUsageException("batch failed");
    var rollbackFailure = new IllegalStateException("rollback failed");
    given(statement.executeBatch()).willThrow(failure);
    willThrow(rollbackFailure).given(transaction).rollback(false);

    assertThatThrownBy(() -> execute(update)).isSameAs(failure);
    assertThat(failure.getSuppressed()).containsExactly(rollbackFailure);
    verify(transaction).rollback(false);
    verify(statement).close();
    verify(transaction).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void sqlFailureRetainsRollbackFailureWhenTranslated(boolean update) throws SQLException {
    var failure = new SQLException("batch failed");
    var rollbackFailure = new IllegalStateException("rollback failed");
    var translated = new PersistenceException("translated", failure);
    given(statement.executeBatch()).willThrow(failure);
    given(repository.translateException(anyString(), isNull(), any(SQLException.class))).willReturn(translated);
    willThrow(rollbackFailure).given(transaction).rollback(false);

    assertThatThrownBy(() -> execute(update)).isSameAs(translated).hasCause(failure);
    assertThat(failure.getSuppressed()).containsExactly(rollbackFailure);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void completedTransactionIsNotRolledBackAfterCommitFailure(boolean update) {
    var failure = new InvalidDataAccessApiUsageException("commit failed");
    willThrow(failure).given(transaction).commit(false);
    given(status.isCompleted()).willReturn(true);

    assertThatThrownBy(() -> execute(update)).isSameAs(failure);
    verify(transaction, never()).rollback(false);
    assertThat(failure.getSuppressed()).isEmpty();
    verify(transaction).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void rollbackThrowingOriginalFailureDoesNotCauseSelfSuppression(boolean update) throws SQLException {
    var failure = new InvalidDataAccessApiUsageException("batch failed");
    given(statement.executeBatch()).willThrow(failure);
    willThrow(failure).given(transaction).rollback(false);

    assertThatThrownBy(() -> execute(update)).isSameAs(failure);
    assertThat(failure.getSuppressed()).isEmpty();
  }

  private void execute(boolean update) {
    Item item = new Item();
    item.id = 1L;
    item.name = "item";
    if (update) {
      manager.updateById(List.of(item));
    }
    else {
      manager.persist(List.of(item));
    }
  }

  @Table("batch_item")
  static class Item {
    public Long id;
    public String name;
  }

}
