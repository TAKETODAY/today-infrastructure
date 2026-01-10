/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

import infra.dao.UncategorizedDataAccessException;

/**
 * Exception thrown when we can't classify an SQLException into
 * one of our generic data access exceptions.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class UncategorizedSQLException extends UncategorizedDataAccessException {

  /** SQL that led to the problem. */
  @Nullable
  private final String sql;

  /**
   * Constructor for UncategorizedSQLException.
   *
   * @param task name of current task
   * @param sql the offending SQL statement
   * @param ex the root cause
   */
  public UncategorizedSQLException(String task, @Nullable String sql, SQLException ex) {
    super("%s; uncategorized SQLException%s; SQL state [%s]; error code [%d]; %s"
            .formatted(task, sql != null ? " for SQL [%s]".formatted(sql) : "", ex.getSQLState(), ex.getErrorCode(), ex.getMessage()), ex);
    this.sql = sql;
  }

  /**
   * Return the underlying SQLException.
   */
  @SuppressWarnings("NullAway")
  public SQLException getSQLException() {
    return (SQLException) getCause();
  }

  /**
   * Return the SQL that led to the problem (if known).
   */
  @Nullable
  public String getSql() {
    return this.sql;
  }

}
