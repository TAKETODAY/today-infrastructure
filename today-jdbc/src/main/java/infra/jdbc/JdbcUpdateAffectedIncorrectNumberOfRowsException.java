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

import infra.dao.IncorrectUpdateSemanticsDataAccessException;

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
