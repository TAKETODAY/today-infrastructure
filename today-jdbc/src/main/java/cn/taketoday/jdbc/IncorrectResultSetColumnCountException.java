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

import cn.taketoday.dao.DataRetrievalFailureException;

/**
 * Data access exception thrown when a result set did not have the correct column count,
 * for example when expecting a single column but getting 0 or more than 1 columns.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.dao.IncorrectResultSizeDataAccessException
 * @since 4.0
 */
public class IncorrectResultSetColumnCountException extends DataRetrievalFailureException {

  private final int expectedCount;

  private final int actualCount;

  /**
   * Constructor for IncorrectResultSetColumnCountException.
   *
   * @param expectedCount the expected column count
   * @param actualCount the actual column count
   */
  public IncorrectResultSetColumnCountException(int expectedCount, int actualCount) {
    super("Incorrect column count: expected %d, actual %d".formatted(expectedCount, actualCount));
    this.expectedCount = expectedCount;
    this.actualCount = actualCount;
  }

  /**
   * Constructor for IncorrectResultCountDataAccessException.
   *
   * @param msg the detail message
   * @param expectedCount the expected column count
   * @param actualCount the actual column count
   */
  public IncorrectResultSetColumnCountException(String msg, int expectedCount, int actualCount) {
    super(msg);
    this.expectedCount = expectedCount;
    this.actualCount = actualCount;
  }

  /**
   * Return the expected column count.
   */
  public int getExpectedCount() {
    return this.expectedCount;
  }

  /**
   * Return the actual column count.
   */
  public int getActualCount() {
    return this.actualCount;
  }

}
