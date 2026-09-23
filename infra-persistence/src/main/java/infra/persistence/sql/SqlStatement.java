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

package infra.persistence.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import infra.jdbc.ParameterBinder;

/**
 * A snapshot of rendered SQL and the binders registered by a {@link SqlBuilder}.
 *
 * <p>Only placeholders added through {@link SqlBuilder#parameter(ParameterBinder)}
 * are represented by these binders; raw SQL fragments may contain other
 * placeholders that must be bound separately.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class SqlStatement {

  private final String sql;

  private final List<ParameterBinder> binders;

  SqlStatement(String sql, List<ParameterBinder> binders) {
    this.sql = sql;
    this.binders = List.copyOf(binders);
  }

  /**
   * Return the rendered SQL text.
   *
   * @return the SQL text
   */
  public String sql() {
    return sql;
  }

  /**
   * Return the number of registered binders.
   *
   * @return the number of binders, not necessarily the number of raw {@code ?} tokens
   */
  public int parameterCount() {
    return binders.size();
  }

  /**
   * Bind registered parameters starting at JDBC index 1.
   *
   * @param statement the prepared statement
   * @throws SQLException if a binder fails
   */
  public void bind(PreparedStatement statement) throws SQLException {
    bind(statement, 1);
  }

  /**
   * Bind registered parameters starting at the given JDBC index.
   *
   * @param statement the prepared statement
   * @param firstIndex the first one-based parameter index
   * @return the index immediately after the last bound parameter
   * @throws IllegalArgumentException if {@code firstIndex} is less than 1
   * @throws SQLException if a binder fails
   */
  public int bind(PreparedStatement statement, int firstIndex) throws SQLException {
    Objects.requireNonNull(statement, "statement");
    if (firstIndex < 1) {
      throw new IllegalArgumentException("First parameter index must be positive");
    }
    int index = firstIndex;
    for (ParameterBinder binder : binders) {
      binder.bind(statement, index++);
    }
    return index;
  }

}
