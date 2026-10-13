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
 * Represents the result of an update operation in the database.
 * This class extends {@link AbstractKeyResult} and provides methods to retrieve
 * information about the affected rows and generated keys after an update or insert operation.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Retrieve the number of affected rows using {@link #getAffectedRows()}.</li>
 *   <li>Access generated keys using methods like {@link #getFirstKey()}, {@link #getKeys()}, and {@link #getKeys(Class)}.</li>
 *   <li>Support for type conversion of generated keys using a {@link infra.core.conversion.ConversionService}.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Execute an update query with returnGeneratedKeys set to true
 * Query query = executionResult.createQuery("INSERT INTO users (name) VALUES ('John Doe')", true);
 * UpdateResult<Long> result = query.executeUpdate();
 *
 * // Retrieve the number of affected rows
 * int affectedRows = result.getAffectedRows();
 * System.out.println("Affected Rows: " + affectedRows);
 *
 * // Retrieve the first generated key
 * Long firstKey = result.getFirstKey();
 * System.out.println("First Generated Key: " + firstKey);
 *
 * // Retrieve all generated keys as a list of Strings
 * List<String> keysAsStrings = result.getKeys(String.class);
 * System.out.println("Generated Keys as Strings: " + keysAsStrings);
 * }</pre>
 *
 * <p><strong>Notes:</strong>
 * <ul>
 *   <li>To fetch generated keys, ensure that the `returnGeneratedKeys` parameter is set to `true`
 *       when creating the query using {@link ExecutionResult#createQuery(String, boolean)}.</li>
 *   <li>If generated keys are not fetched, calling methods like {@link #getFirstKey()} or {@link #getKeys()}
 *       will throw a {@link GeneratedKeysException}.</li>
 *   <li>Type conversion for generated keys requires a valid {@link infra.core.conversion.ConversionService} instance.</li>
 * </ul>
 *
 * @param <T> the type of generated keys
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/17 10:28
 */
public class UpdateResult<T extends @Nullable Object> extends AbstractKeyResult<T> {

  private final @Nullable Integer affectedRows;

  /**
   * Creates a new {@code UpdateResult} with the given affected rows and connection.
   *
   * @param affectedRows the number of affected rows, may be {@code null} if not yet executed
   * @param connection the {@link JdbcConnection} used to access the database
   */
  public UpdateResult(@Nullable Integer affectedRows, JdbcConnection connection) {
    super(connection);
    this.affectedRows = affectedRows;
  }

  /**
   * @return the number of rows updated or deleted
   * @throws PersistenceException if the update has not been executed yet
   */
  public int getAffectedRows() {
    if (affectedRows == null) {
      throw new PersistenceException(
              "It is required to call executeUpdate() method before calling getAffectedRows().");
    }
    return affectedRows;
  }

}