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

import cn.taketoday.dao.IncorrectUpdateSemanticsDataAccessException;

/**
 * Exception thrown when a JDBC update affects an unexpected number of rows.
 * Typically we expect an update to affect a single row, meaning it's an
 * error if it affects multiple rows.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JdbcUpdateAffectedIncorrectNumberOfRowsException extends IncorrectUpdateSemanticsDataAccessException {

  /** Number of rows that should have been affected. */
  private final int expected;

  /** Number of rows that actually were affected. */
  private final int actual;

  /**
   * Constructor for JdbcUpdateAffectedIncorrectNumberOfRowsException.
   *
   * @param sql the SQL we were trying to execute
   * @param expected the expected number of rows affected
   * @param actual the actual number of rows affected
   */
  public JdbcUpdateAffectedIncorrectNumberOfRowsException(String sql, int expected, int actual) {
    super("SQL update '%s' affected %s rows, not %s as expected".formatted(sql, actual, expected));
    this.expected = expected;
    this.actual = actual;
  }

  /**
   * Return the number of rows that should have been affected.
   */
  public int getExpectedRowsAffected() {
    return this.expected;
  }

  /**
   * Return the number of rows that have actually been affected.
   */
  public int getActualRowsAffected() {
    return this.actual;
  }

  @Override
  public boolean wasDataUpdated() {
    return getActualRowsAffected() > 0;
  }

}
