/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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