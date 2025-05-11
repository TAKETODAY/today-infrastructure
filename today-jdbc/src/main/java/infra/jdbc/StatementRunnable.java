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

import infra.lang.Nullable;

/**
 * Represents a method with a {@link JdbcConnection} and an optional argument.
 * Implementations of this interface be used as a parameter to one of the
 * {@link RepositoryManager#runInTransaction(StatementRunnable) RepositoryManager.runInTransaction}
 * overloads, to run code safely in a transaction.
 *
 * @param <T> Argument type
 */
public interface StatementRunnable<T> {

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
  void run(JdbcConnection connection, @Nullable T argument) throws Throwable;

}
