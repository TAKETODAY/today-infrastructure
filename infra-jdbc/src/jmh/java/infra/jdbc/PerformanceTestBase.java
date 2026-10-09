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

import java.sql.SQLException;

/**
 * A thread-confined single-row query with cyclic identifiers and trial resources.
 */
public abstract class PerformanceTestBase implements AutoCloseable {

  /** Initialize the implementation before validation and measurement. */
  public void initialize() throws Exception {
    init();
  }

  /** Acquire this worker's query resources. */
  public abstract void init() throws Exception;

  int input = 1;

  /** Execute a query using the next identifier in the fixture's range. */
  public Object run() throws SQLException {
    int id = input;
    input = input == SelectBenchmark.ROW_COUNT ? 1 : input + 1;
    return run(id);
  }

  /**
   * Execute a single-row query.
   * @param input the row identifier
   * @return the mapped row, or {@code null} when absent
   */
  public abstract Object run(int input) throws SQLException;

  /** Release resources, including those acquired by a partially completed initialization. */
  @Override
  public abstract void close() throws Exception;

  String getName() {
    return getClass().getSimpleName();
  }

}
