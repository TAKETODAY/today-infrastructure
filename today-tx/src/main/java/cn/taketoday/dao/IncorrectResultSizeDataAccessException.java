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
 * Data access exception thrown when a result was not of the expected size,
 * for example when expecting a single row but getting 0 or more than 1 rows.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EmptyResultDataAccessException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class IncorrectResultSizeDataAccessException extends DataRetrievalFailureException {

  private final int expectedSize;

  private final int actualSize;

  /**
   * Constructor for IncorrectResultSizeDataAccessException.
   *
   * @param expectedSize the expected result size
   */
  public IncorrectResultSizeDataAccessException(int expectedSize) {
    super("Incorrect result size: expected " + expectedSize);
    this.expectedSize = expectedSize;
    this.actualSize = -1;
  }

  /**
   * Constructor for IncorrectResultSizeDataAccessException.
   *
   * @param expectedSize the expected result size
   * @param actualSize the actual result size (or -1 if unknown)
   */
  public IncorrectResultSizeDataAccessException(int expectedSize, int actualSize) {
    super("Incorrect result size: expected " + expectedSize + ", actual " + actualSize);
    this.expectedSize = expectedSize;
    this.actualSize = actualSize;
  }

  /**
   * Constructor for IncorrectResultSizeDataAccessException.
   *
   * @param msg the detail message
   * @param expectedSize the expected result size
   */
  public IncorrectResultSizeDataAccessException(String msg, int expectedSize) {
    super(msg);
    this.expectedSize = expectedSize;
    this.actualSize = -1;
  }

  /**
   * Constructor for IncorrectResultSizeDataAccessException.
   *
   * @param msg the detail message
   * @param expectedSize the expected result size
   * @param ex the wrapped exception
   */
  public IncorrectResultSizeDataAccessException(String msg, int expectedSize, Throwable ex) {
    super(msg, ex);
    this.expectedSize = expectedSize;
    this.actualSize = -1;
  }

  /**
   * Constructor for IncorrectResultSizeDataAccessException.
   *
   * @param msg the detail message
   * @param expectedSize the expected result size
   * @param actualSize the actual result size (or -1 if unknown)
   */
  public IncorrectResultSizeDataAccessException(String msg, int expectedSize, int actualSize) {
    super(msg);
    this.expectedSize = expectedSize;
    this.actualSize = actualSize;
  }

  /**
   * Constructor for IncorrectResultSizeDataAccessException.
   *
   * @param msg the detail message
   * @param expectedSize the expected result size
   * @param actualSize the actual result size (or -1 if unknown)
   * @param ex the wrapped exception
   */
  public IncorrectResultSizeDataAccessException(String msg, int expectedSize, int actualSize, Throwable ex) {
    super(msg, ex);
    this.expectedSize = expectedSize;
    this.actualSize = actualSize;
  }

  /**
   * Return the expected result size.
   */
  public int getExpectedSize() {
    return this.expectedSize;
  }

  /**
   * Return the actual result size (or -1 if unknown).
   */
  public int getActualSize() {
    return this.actualSize;
  }

}
