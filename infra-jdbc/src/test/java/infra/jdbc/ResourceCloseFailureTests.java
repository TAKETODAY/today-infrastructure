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
  void observerFailureDoesNotReplaceOperationFailure() {
    RepositoryManager manager = new RepositoryManager(mock(DataSource.class));
    Throwable operationFailure = new IllegalStateException("operation failed");
    Throwable closeFailure = new SQLException("close failed");
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
    willThrow(new SQLException("result set close failed")).given(resultSet).close();
    willThrow(new SQLException("statement close failed")).given(statement).close();
    willThrow(new SQLException("connection close failed")).given(connection).close();
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
