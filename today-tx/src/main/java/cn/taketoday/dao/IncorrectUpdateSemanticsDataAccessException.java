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

package cn.taketoday.dao;

/**
 * Data access exception thrown when something unintended appears to have
 * happened with an update, but the transaction hasn't already been rolled back.
 * Thrown, for example, when we wanted to update 1 row in an RDBMS but actually
 * updated 3.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class IncorrectUpdateSemanticsDataAccessException extends InvalidDataAccessResourceUsageException {

  /**
   * Constructor for IncorrectUpdateSemanticsDataAccessException.
   *
   * @param msg the detail message
   */
  public IncorrectUpdateSemanticsDataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for IncorrectUpdateSemanticsDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause from the underlying API, such as JDBC
   */
  public IncorrectUpdateSemanticsDataAccessException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Return whether data was updated.
   * If this method returns false, there's nothing to roll back.
   * <p>The default implementation always returns true.
   * This can be overridden in subclasses.
   */
  public boolean wasDataUpdated() {
    return true;
  }

}
