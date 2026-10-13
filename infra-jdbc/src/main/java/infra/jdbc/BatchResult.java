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

import java.sql.PreparedStatement;

/**
 * Batch execution result
 *
 * <p>Generated keys of a batch execution can be accessed through the inherited
 * {@link AbstractKeyResult} API, such as {@link #getFirstKey()} and {@link #getKeys()}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/17 11:25
 */
public class BatchResult extends AbstractKeyResult<Object> {

  private int @Nullable [] batchResult;

  private @Nullable Integer affectedRows;

  /**
   * Creates a new {@code BatchResult} associated with the given connection.
   *
   * @param connection the {@link JdbcConnection} used to access the database
   */
  public BatchResult(JdbcConnection connection) {
    super(connection);
  }

  /**
   * Get last batch execution result
   * <p>
   *
   * Maybe explicitly execution
   *
   * @see NamedQuery#addToBatch()
   * @see PreparedStatement#executeBatch()
   */
  public int[] getLastBatchResult() {
    if (batchResult == null) {
      throw new PersistenceException(
              "It is required to call executeBatch() method before calling getLastBatchResult().");
    }
    return batchResult;
  }

  void setBatchResult(int[] batchResult) {
    this.batchResult = batchResult;
    if (this.affectedRows == null) {
      this.affectedRows = batchResult.length;
    }
    else {
      this.affectedRows += batchResult.length;
    }
  }

  /**
   * @return the number of rows updated or deleted
   * @throws PersistenceException if the batch has not been executed yet
   */
  public int getAffectedRows() {
    if (affectedRows == null) {
      throw new PersistenceException(
              "It is required to call executeBatch() method before calling getAffectedRows().");
    }
    return affectedRows;
  }

}