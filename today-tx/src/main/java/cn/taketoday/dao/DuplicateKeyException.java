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
 * results in violation of a primary key or unique constraint.
 * Note that this is not necessarily a purely relational concept;
 * unique primary keys are required by most database types.
 *
 * <p>Consider handling the general {@link DataIntegrityViolationException}
 * instead, semantically including a wider range of constraint violations.
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DuplicateKeyException extends DataIntegrityViolationException {

  /**
   * Constructor for DuplicateKeyException.
   *
   * @param msg the detail message
   */
  public DuplicateKeyException(String msg) {
    super(msg);
  }

  /**
   * Constructor for DuplicateKeyException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public DuplicateKeyException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
