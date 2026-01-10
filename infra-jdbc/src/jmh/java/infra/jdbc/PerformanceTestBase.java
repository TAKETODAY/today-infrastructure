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
 * Basically a {@link Runnable} with an Integer input.
 */
public abstract class PerformanceTestBase implements AutoCloseable {

  public void initialize() throws Exception {
    init();
  }

  public abstract void init() throws Exception;

  int input = 1;

  public Object run() throws SQLException {
    return run(input++);
  }

  public abstract Object run(int input) throws SQLException;

  public abstract void close() throws Exception;

  String getName() {
    return getClass().getSimpleName();
  }

}
