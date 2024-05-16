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

import cn.taketoday.dao.InvalidDataAccessResourceUsageException;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when a ResultSet has been accessed in an invalid fashion.
 * Such exceptions always have a {@code java.sql.SQLException} root cause.
 *
 * <p>This typically happens when an invalid ResultSet column index or name
 * has been specified. Also thrown by disconnected SqlRowSets.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BadSqlGrammarException
 * @see cn.taketoday.jdbc.support.rowset.SqlRowSet
 * @since 4.0
 */
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

  @Nullable
  private final String sql;

  /**
   * Constructor for InvalidResultSetAccessException.
   *
   * @param task name of current task
   * @param sql the offending SQL statement
   * @param ex the root cause
   */
  public InvalidResultSetAccessException(String task, String sql, SQLException ex) {
    super(task + "; invalid ResultSet access for SQL [" + sql + "]", ex);
    this.sql = sql;
  }

  /**
   * Constructor for InvalidResultSetAccessException.
   *
   * @param ex the root cause
   */
  public InvalidResultSetAccessException(SQLException ex) {
    super(ex.getMessage(), ex);
    this.sql = null;
  }

  /**
   * Return the wrapped SQLException.
   */
  public SQLException getSQLException() {
    return (SQLException) getCause();
  }

  /**
   * Return the SQL that caused the problem.
   *
   * @return the offending SQL, if known
   */
  @Nullable
  public String getSql() {
    return this.sql;
  }

}
