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

import cn.taketoday.lang.Nullable;

/**
 * Root of the hierarchy of data access exceptions that are considered transient -
 * where a previously failed operation might be able to succeed when the operation
 * is retried without any intervention by application-level functionality.
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.sql.SQLTransientException
 * @since 4.0
 */
public abstract class TransientDataAccessException extends DataAccessException {

  /**
   * Constructor for TransientDataAccessException.
   *
   * @param msg the detail message
   */
  public TransientDataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for TransientDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using a underlying
   * data access API such as JDBC)
   */
  public TransientDataAccessException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
