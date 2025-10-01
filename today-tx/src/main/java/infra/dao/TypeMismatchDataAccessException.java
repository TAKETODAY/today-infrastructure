/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on mismatch between Java type and database type:
 * for example on an attempt to set an object of the wrong type
 * in an RDBMS column.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class TypeMismatchDataAccessException extends InvalidDataAccessResourceUsageException {

  /**
   * Constructor for TypeMismatchDataAccessException.
   *
   * @param msg the detail message
   */
  public TypeMismatchDataAccessException(@Nullable String msg) {
    this(msg, null);
  }

  /**
   * Constructor for TypeMismatchDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public TypeMismatchDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
