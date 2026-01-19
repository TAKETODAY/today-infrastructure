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

import org.jspecify.annotations.Nullable;

/**
 * Represents a method with a {@link JdbcConnection} and an optional argument.
 * Implementations of this interface be used as a parameter to one of the
 * {@link RepositoryManager#runInTransaction(StatementRunnable) RepositoryManager.runInTransaction}
 * overloads, to run code safely in a transaction.
 *
 * @param <T> Argument type
 */
public interface StatementRunnable<T extends @Nullable Object> {

  /**
   * Executes the statement logic within a transaction using the provided JDBC connection.
   * This method is typically used as part of a transactional operation managed by
   * {@link RepositoryManager#runInTransaction(StatementRunnable)}.
   *
   * <p>Example usage:
   * <pre>{@code
   * StatementRunnable<Integer> runnable = (connection, argument) -> {
   *   // Perform database operations using the connection
   *   String sql = "INSERT INTO example_table (value) VALUES (?)";
   *   try (PreparedStatement stmt = connection.prepareStatement(sql)) {
   *     stmt.setInt(1, argument);
   *     stmt.executeUpdate();
   *   }
   * };
   *
   * RepositoryManager manager = new RepositoryManager();
   * manager.runInTransaction(runnable, 42); // Passes 42 as the argument
   * }</pre>
   *
   * @param connection the {@link JdbcConnection} to use for executing statements.
   * This connection is managed by the transaction and should not
   * be closed manually.
   * @param argument the optional argument passed to the statement logic.
   * Can be {@code null} if no argument is required.
   * @throws Throwable if an error occurs during the execution of the statement logic.
   * This exception will be propagated to the caller of the
   * transaction method.
   */
  void run(JdbcConnection connection, T argument) throws Throwable;

}
