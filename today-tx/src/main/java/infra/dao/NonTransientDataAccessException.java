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

package infra.dao;

import infra.lang.Nullable;

/**
 * Root of the hierarchy of data access exceptions that are considered non-transient -
 * where a retry of the same operation would fail unless the cause of the Exception
 * is corrected.
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.sql.SQLNonTransientException
 * @since 4.0
 */
public abstract class NonTransientDataAccessException extends DataAccessException {

  /**
   * Constructor for NonTransientDataAccessException.
   *
   * @param msg the detail message
   */
  public NonTransientDataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for NonTransientDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using a underlying
   * data access API such as JDBC)
   */
  public NonTransientDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
