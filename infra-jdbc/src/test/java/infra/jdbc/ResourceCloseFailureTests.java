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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.DataSource;

import infra.jdbc.config.RepositoryManagerAutoConfiguration;
import infra.jdbc.config.RepositoryProperties;
import infra.jdbc.type.TypeHandlerManager;
import infra.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResourceCloseFailureTests {

  @Test
  void borrowedWrapperPreservesConnectionStateAndOwnership() throws SQLException {
    Connection root = mock(Connection.class);
    given(root.getAutoCommit()).willReturn(false);
    given(root.getTransactionIsolation()).willReturn(Connection.TRANSACTION_SERIALIZABLE);
    DataSource dataSource = mock(DataSource.class);
    RepositoryManager manager = new RepositoryManager(dataSource);
    JdbcConnection connection = manager.wrap(root);
    assertThat(connection.getNativeConnection()).isSameAs(root);
    assertThat(connection.getNativeConnection().getAutoCommit()).isFalse();
    assertThat(connection.getNativeConnection().getTransactionIsolation())
            .isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
    org.assertj.core.api.Assertions.assertThatThrownBy(connection::beginTransaction)
            .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
    connection.close();
    connection.close();
    org.mockito.Mockito.verify(dataSource, org.mockito.Mockito.never()).getConnection();
    org.mockito.Mockito.verify(root, org.mockito.Mockito.never()).close();
    org.mockito.Mockito.verify(root, org.mockito.Mockito.never()).commit();
    org.mockito.Mockito.verify(root, org.mockito.Mockito.never()).rollback();
  }

  @Test
  void borrowedWrapperRejectsExistingQueryAfterClose() throws SQLException {
    Connection root = mock(Connection.class);
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    JdbcConnection connection = manager.wrap(root);
    Query query = connection.createQuery("select 1");
    NamedQuery namedQuery = connection.createNamedQuery("update items set id = 1");
    connection.close();

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> query.fetchFirst(Integer.class))
            .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(namedQuery::executeUpdate)
            .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> connection.registerStatement(mock(PreparedStatement.class)))
            .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
    org.mockito.Mockito.verify(root, org.mockito.Mockito.never()).prepareStatement(org.mockito.ArgumentMatchers.anyString());
    org.mockito.Mockito.verify(root, org.mockito.Mockito.never()).close();
  }

  @Test
  void closedWrapperRejectsQueryWithCachedStatement() throws SQLException {
    Connection root = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    given(root.prepareStatement("update items set id = 1")).willReturn(statement);
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    JdbcConnection connection = manager.wrap(root);
    Query query = connection.createQuery("update items set id = 1");
    query.buildStatement();
    connection.close();
    org.assertj.core.api.Assertions.assertThatThrownBy(query::executeUpdate)
            .isInstanceOf(infra.dao.InvalidDataAccessApiUsageException.class);
    org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).executeUpdate();
    verify(statement).close();
  }

  @Test
  void closeErrorIsReportedAndSuppressedOnOperationFailure() {
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    var failures = new ArrayList<ResourceCloseFailure>();
    manager.setResourceCloseFailureListener(failures::add);
    Error error = new AssertionError("close error");
    Throwable operationFailure = new IllegalStateException("operation failed");
    AutoCloseable resource = () -> { throw error; };
    manager.closeResource(resource, "select 1", operationFailure);
    assertThat(operationFailure.getSuppressed()).containsExactly(error);
    assertThat(failures).singleElement().satisfies(failure -> {
      assertThat(failure.exception()).isSameAs(error);
      assertThat(failure.operationFailure()).isSameAs(operationFailure);
      assertThat(failure.sql()).isEqualTo("select 1");
    });
  }

  @Test
  void observerErrorIsSuppressedOnCloseFailure() {
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    Error error = new AssertionError("observer error");
    manager.setResourceCloseFailureListener(failure -> { throw error; });
    SQLException closeFailure = new SQLException("close failed");
    AutoCloseable resource = () -> { throw closeFailure; };
    manager.closeResource(resource, null, null);
    assertThat(closeFailure.getSuppressed()).containsExactly(error);
  }

  @Test
  void interruptedCloseRestoresInterruptFlagAndReportsOtherResource() {
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    var failures = new ArrayList<ResourceCloseFailure>();
    manager.setResourceCloseFailureListener(failures::add);
    InterruptedException interruption = new InterruptedException("close interrupted");
    AutoCloseable resource = () -> { throw interruption; };
    try {
      manager.closeResource(resource, null, null);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      assertThat(failures).singleElement().satisfies(failure -> {
        assertThat(failure.resourceType()).isEqualTo(ResourceCloseFailure.ResourceType.OTHER);
        assertThat(failure.exception()).isSameAs(interruption);
      });
    }
    finally {
      Thread.interrupted();
    }
  }

  @Test
  void connectionCleanupContinuesAfterStatementErrorAndReportsIt() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection root = mock(Connection.class);
    given(dataSource.getConnection()).willReturn(root);
    RepositoryManager manager = new RepositoryManager(dataSource);
    var failures = new ArrayList<ResourceCloseFailure>();
    manager.setResourceCloseFailureListener(failures::add);
    PreparedStatement statement = mock(PreparedStatement.class);
    Error error = new AssertionError("statement close error");
    willThrow(error).given(statement).close();
    JdbcConnection connection = manager.open();
    connection.registerStatement(statement);
    connection.close();
    assertThat(failures).singleElement().satisfies(failure -> {
      assertThat(failure.resourceType()).isEqualTo(ResourceCloseFailure.ResourceType.STATEMENT);
      assertThat(failure.exception()).isSameAs(error);
    });
    verify(root).close();
  }

  @Test
  void observerFailureDoesNotReplaceOperationFailure() {
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    Throwable operationFailure = new IllegalStateException("operation failed");
    Exception closeFailure = new SQLException("close failed");
    RuntimeException listenerFailure = new IllegalStateException("listener failed");
    var context = new ResourceCloseFailure(ResourceCloseFailure.ResourceType.RESULT_SET,
            "select 1", closeFailure, operationFailure);
    manager.setResourceCloseFailureListener(failure -> { throw listenerFailure; });
    manager.reportResourceCloseFailure(context);
    assertThat(operationFailure.getSuppressed()).containsExactly(closeFailure);
    assertThat(closeFailure.getSuppressed()).containsExactly(listenerFailure);
  }

  @Test
  void scalarReportsAllCloseFailuresAndPreservesResult() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.prepareStatement("select 1")).willReturn(statement);
    given(statement.executeQuery()).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getInt(1)).willReturn(1);
    willThrow(new AssertionError("result set close failed")).given(resultSet).close();
    willThrow(new AssertionError("statement close failed")).given(statement).close();
    willThrow(new AssertionError("connection close failed")).given(connection).close();
    RepositoryManager manager = new RepositoryManager(dataSource);
    var failures = new ArrayList<ResourceCloseFailure>();
    manager.setResourceCloseFailureListener(failures::add);
    assertThat(manager.createQuery("select 1").scalar(Integer.class)).isEqualTo(1);
    assertThat(failures).extracting(ResourceCloseFailure::resourceType).containsExactly(
            ResourceCloseFailure.ResourceType.RESULT_SET,
            ResourceCloseFailure.ResourceType.STATEMENT,
            ResourceCloseFailure.ResourceType.CONNECTION);
    assertThat(failures.get(0).sql()).isEqualTo("select 1");
    verify(resultSet).close();
    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void autoConfigurationUsesCustomObserver() {
    ResourceCloseFailureListener listener = failure -> { };
    RepositoryManager manager = RepositoryManagerAutoConfiguration.repositoryManager(
            mock(DataSource.class), mock(PlatformTransactionManager.class),
            TypeHandlerManager.sharedInstance, null, new RepositoryProperties(), listener);
    assertThat(manager.getResourceCloseFailureListener()).isSameAs(listener);
  }

}
