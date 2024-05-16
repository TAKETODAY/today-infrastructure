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
 * Exception to be thrown on a query timeout. This could have different causes depending on
 * the database API in use but most likely thrown after the database interrupts or stops
 * the processing of a query before it has completed.
 *
 * <p>This exception can be thrown by user code trapping the native database exception or
 * by exception translation.
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class QueryTimeoutException extends TransientDataAccessException {

  /**
   * Constructor for QueryTimeoutException.
   *
   * @param msg the detail message
   */
  public QueryTimeoutException(String msg) {
    super(msg);
  }

  /**
   * Constructor for QueryTimeoutException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public QueryTimeoutException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
