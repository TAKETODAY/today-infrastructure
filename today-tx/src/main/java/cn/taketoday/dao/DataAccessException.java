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

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

/**
 * Root of the hierarchy of data access exceptions discussed in
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>.
 * Please see Chapter 9 of this book for detailed discussion of the
 * motivation for this package.
 *
 * <p>This exception hierarchy aims to let user code find and handle the
 * kind of error encountered without knowing the details of the particular
 * data access API in use (e.g. JDBC). Thus it is possible to react to an
 * optimistic locking failure without knowing that JDBC is being used.
 *
 * <p>As this class is a runtime exception, there is no need for user code
 * to catch it or subclasses if any error is to be considered fatal
 * (the usual case).
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public abstract class DataAccessException extends NestedRuntimeException {

  /**
   * Constructor for DataAccessException.
   */
  public DataAccessException() { }

  /**
   * Constructor for DataAccessException.
   *
   * @param msg the detail message
   */
  public DataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for DataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using a underlying
   * data access API such as JDBC)
   */
  public DataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

  /**
   * Constructor for DataAccessException.
   *
   * @param cause the root cause (usually from using a underlying
   * data access API such as JDBC)
   */
  public DataAccessException(Throwable cause) {
    super(cause);
  }

}
