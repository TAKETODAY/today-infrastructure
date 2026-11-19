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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.QueryTimeoutException;
import infra.jdbc.datasource.TransactionAwareDataSourceProxy;
import infra.jdbc.support.JdbcTransactionManager;
import infra.transaction.annotation.Isolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RepositoryManagerCallbackTests {

  private final DataSource dataSource = mock();

  private final Connection connection = mock();

  private final RepositoryManager manager = new RepositoryManager(dataSource);

  @BeforeEach
  void configureConnection() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
  }

  @Test
  void commitsResultCallbackAndReturnsItsValue() throws SQLException {
    given(connection.getAutoCommit()).willReturn(true);
    ResultStatementRunnable<Integer, Object> callback = (jdbc, argument) -> {
      assertThat(jdbc.getTransaction()).isNotNull();
      assertThat(argument).isNull();
      return 42;
    };

    assertThat(manager.runInTransaction(callback)).isEqualTo(42);

    var order = inOrder(connection);
    order.verify(connection).setAutoCommit(false);
    order.verify(connection).commit();
    order.verify(connection).setAutoCommit(true);
    order.verify(connection).close();
    verify(connection, never()).rollback();
  }

  @ParameterizedTest
  @MethodSource("callbackFailures")
  void rollsBackResultCallbackBeforePropagatingFailure(Throwable failure, Class<? extends Throwable> expectedType)
          throws SQLException {
    ResultStatementRunnable<Object, String> callback = (jdbc, argument) -> {
      assertThat(argument).isEqualTo("input");
      throw failure;
    };

    assertThatThrownBy(() -> manager.runInTransaction(callback, "input", Isolation.SERIALIZABLE))
            .isInstanceOf(expectedType).satisfies(actual -> assertOriginalFailure(actual, failure, expectedType));

    var order = inOrder(connection);
    order.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    order.verify(connection).rollback();
    order.verify(connection).close();
    verify(connection, never()).commit();
  }

  @ParameterizedTest
  @MethodSource("connectionCallbackFailures")
  void releasesConnectionWhenResultCallbackFails(Throwable failure, Class<? extends Throwable> expectedType)
          throws SQLException {
    ResultStatementRunnable<Object, Object> callback = (jdbc, argument) -> {
      throw failure;
    };

    assertThatThrownBy(() -> manager.withConnection(callback))
            .isInstanceOf(expectedType).satisfies(actual -> assertOriginalFailure(actual, failure, expectedType));

    verify(connection).close();
  }

  @ParameterizedTest
  @MethodSource("connectionCallbackFailures")
  void releasesConnectionWhenVoidCallbackFails(Throwable failure, Class<? extends Throwable> expectedType)
          throws SQLException {
    StatementRunnable<Object> callback = (jdbc, argument) -> {
      throw failure;
    };

    assertThatThrownBy(() -> manager.withConnection(callback))
            .isInstanceOf(expectedType).satisfies(actual -> assertOriginalFailure(actual, failure, expectedType));

    verify(connection).close();
  }

  @Test
  void validatesTransactionManagerAgainstUnwrappedDataSource() {
    var proxy = new TransactionAwareDataSourceProxy(dataSource);
    var matching = new JdbcTransactionManager(dataSource);
    var mismatched = new JdbcTransactionManager(mock(DataSource.class));

    assertThat(new RepositoryManager(proxy, matching).getTransactionManager()).isSameAs(matching);
    assertThatThrownBy(() -> new RepositoryManager(proxy, mismatched))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("same DataSource");
  }

  private static Stream<Arguments> connectionCallbackFailures() {
    return Stream.of(
            Arguments.of(new InvalidDataAccessApiUsageException("bad input"), InvalidDataAccessApiUsageException.class),
            Arguments.of(new SQLTimeoutException("driver timeout"), QueryTimeoutException.class),
            Arguments.of(new IllegalStateException("callback failed"), PersistenceException.class));
  }

  private static Stream<Arguments> callbackFailures() {
    return Stream.concat(connectionCallbackFailures(),
            Stream.of(Arguments.of(new AssertionError("callback error"), AssertionError.class)));
  }

  private static void assertOriginalFailure(Throwable actual, Throwable original, Class<? extends Throwable> expectedType) {
    if (expectedType.isInstance(original)) {
      assertThat(actual).isSameAs(original);
    }
    else {
      assertThat(actual).hasCause(original);
    }
  }

}
