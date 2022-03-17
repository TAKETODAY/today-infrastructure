/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.dao;

/**
 * Data access exception thrown when a result was expected to have at least
 * one row (or element) but zero rows (or elements) were actually returned.
 *
 * @author Juergen Hoeller
 * @see IncorrectResultSizeDataAccessException
 * @since 2.0
 */
@SuppressWarnings("serial")
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
