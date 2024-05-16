/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc;

import java.sql.SQLException;

import cn.taketoday.dao.UncategorizedDataAccessException;
import cn.taketoday.lang.Nullable;

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
