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
 * Exception thrown when an attempt to insert or update data
 * results in violation of an integrity constraint. Note that this
 * is not purely a relational concept; integrity constraints such
 * as unique primary keys are required by most database types.
 *
 * <p>Serves as a superclass for more specific exceptions, e.g.
 * {@link DuplicateKeyException}. However, it is generally
 * recommended to handle {@code DataIntegrityViolationException}
 * itself instead of relying on specific exception subclasses.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class DataIntegrityViolationException extends NonTransientDataAccessException {

  /**
   * Constructor for DataIntegrityViolationException.
   *
   * @param msg the detail message
   */
  public DataIntegrityViolationException(String msg) {
    super(msg);
  }

  /**
   * Constructor for DataIntegrityViolationException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public DataIntegrityViolationException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
