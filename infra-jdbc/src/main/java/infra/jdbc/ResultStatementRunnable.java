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
 * Represents a function that accepts a {@link JdbcConnection} and an optional argument.
 * Implementations of this interface can be used as a parameter to one of the
 * {@link RepositoryManager#runInTransaction(ResultStatementRunnable)}
 * overloads, to execute code safely within a transaction.
 *
 * @param <V> the return value type
 * @param <P> the parameter type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ResultStatementRunnable<V extends @Nullable Object, P extends @Nullable Object> {

  /**
   * Executes a statement with the given connection and argument.
   *
   * @param connection the JDBC connection to use for executing the statement
   * @param argument an optional argument to pass to the statement execution
   * @return the result of the statement execution
   * @throws Throwable if any error occurs during the statement execution
   */
  V run(JdbcConnection connection, P argument) throws Throwable;
}
