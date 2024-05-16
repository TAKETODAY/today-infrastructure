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

import java.sql.SQLWarning;

import cn.taketoday.dao.UncategorizedDataAccessException;

/**
 * Exception thrown when we're not ignoring {@link java.sql.SQLWarning SQLWarnings}.
 *
 * <p>If an SQLWarning is reported, the operation completed, so we will need
 * to explicitly roll it back if we're not happy when looking at the warning.
 * We might choose to ignore (and log) the warning, or to wrap and throw it
 * in the shape of this SQLWarningException instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.jdbc.core.JdbcTemplate#setIgnoreWarnings
 * @since 4.0
 */
public class SQLWarningException extends UncategorizedDataAccessException {

  /**
   * Constructor for SQLWarningException.
   *
   * @param msg the detail message
   * @param ex the JDBC warning
   */
  public SQLWarningException(String msg, SQLWarning ex) {
    super(msg, ex);
  }

  /**
   * Return the underlying {@link SQLWarning}.
   */
  public SQLWarning getSQLWarning() {
    return (SQLWarning) getCause();
  }

}
