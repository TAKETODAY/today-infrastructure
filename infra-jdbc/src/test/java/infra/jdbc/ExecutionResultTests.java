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

import java.sql.SQLException;

import infra.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 11:36
 */
class ExecutionResultTests {

  private final RepositoryManager repositoryManager = new RepositoryManager("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");

  @Test
  void shouldCreateNamedQuery() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    NamedQuery query = executionResult.createNamedQuery("SELECT * FROM users");

    assertThat(query).isNotNull();
    assertThat(query).extracting("parsedQuery").isEqualTo("SELECT * FROM users");
  }

  @Test
  void shouldCreateNamedQueryWithGeneratedKeys() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    NamedQuery query = executionResult.createNamedQuery("INSERT INTO users (name) VALUES (:name)", true);

    assertThat(query).isNotNull();
    assertThat(query).extracting("parsedQuery").isEqualTo("INSERT INTO users (name) VALUES (?)");
  }

  @Test
  void shouldCreateNamedQueryWithColumnNames() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    NamedQuery query = executionResult.createNamedQuery("INSERT INTO users (name) VALUES (:name)", "id", "created_at");

    assertThat(query).isNotNull();
    assertThat(query).extracting("parsedQuery").isEqualTo("INSERT INTO users (name) VALUES (?)");
  }

  @Test
  void shouldGetRepositoryManager() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    RepositoryManager result = executionResult.getManager();

    assertThat(result).isSameAs(repositoryManager);
  }

  @Test
  void shouldGetConnection() {
    JdbcConnection connection = new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource());
    ExecutionResult executionResult = new TestExecutionResult(connection);

    JdbcConnection result = executionResult.getConnection();

    assertThat(result).isSameAs(connection);
  }

  @Test
  void shouldTranslateException() throws SQLException {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    SQLException sqlException = new SQLException("Test SQL exception");

    DataAccessException translatedException = executionResult.translateException("Test task", sqlException);

    assertThat(translatedException).isNotNull();
    assertThat(translatedException.getMessage()).contains("Test task");
    assertThat(translatedException.getCause()).isSameAs(sqlException);
  }

  @Test
  void shouldCreateQueryWithoutGeneratedKeys() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    Query query = executionResult.createQuery("SELECT * FROM users");
    assertThat(query).isNotNull();
  }

  @Test
  void shouldCreateQueryWithGeneratedKeysFlag() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    Query query = executionResult.createQuery("INSERT INTO users (name) VALUES (?)", true);

    assertThat(query).isNotNull();
  }

  @Test
  void shouldCommitTransaction() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource(), true));
    assertThatNoException().isThrownBy(executionResult::commit);
  }

  @Test
  void shouldCommitTransactionWithCloseConnectionFlag() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource(), true));
    assertThatNoException().isThrownBy(() -> executionResult.commit(true));
  }

  @Test
  void shouldRollbackTransaction() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource(), true));
    assertThatNoException().isThrownBy(executionResult::rollback);
  }

  @Test
  void shouldRollbackTransactionWithCloseConnectionFlag() {
    ExecutionResult executionResult = new TestExecutionResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource(), true));

    assertThatNoException().isThrownBy(() -> executionResult.rollback(true));
  }

  private static class TestExecutionResult extends ExecutionResult {
    public TestExecutionResult(JdbcConnection connection) {
      super(connection);
    }
  }

}