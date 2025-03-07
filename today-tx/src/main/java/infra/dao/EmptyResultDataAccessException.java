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

/**
 * Data access exception thrown when a result was expected to have at least
 * one row (or element) but zero rows (or elements) were actually returned.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see IncorrectResultSizeDataAccessException
 * @since 4.0
 */
public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {

  /**
   * Constructor for EmptyResultDataAccessException.
   *
   * @param expectedSize the expected result size
   */
  public EmptyResultDataAccessException(int expectedSize) {
    super(expectedSize, 0);
  }

  /**
   * Constructor for EmptyResultDataAccessException.
   *
   * @param msg the detail message
   * @param expectedSize the expected result size
   */
  public EmptyResultDataAccessException(String msg, int expectedSize) {
    super(msg, expectedSize, 0);
  }

  /**
   * Constructor for EmptyResultDataAccessException.
   *
   * @param msg the detail message
   * @param expectedSize the expected result size
   * @param ex the wrapped exception
   */
  public EmptyResultDataAccessException(String msg, int expectedSize, Throwable ex) {
    super(msg, expectedSize, 0, ex);
  }

}
