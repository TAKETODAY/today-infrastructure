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

package cn.taketoday.jdbc.support;

import cn.taketoday.core.NestedCheckedException;

/**
 * Exception indicating that something went wrong during JDBC meta-data lookup.
 *
 * <p>This is a checked exception since we want it to be caught, logged and
 * handled rather than cause the application to fail. Failure to read JDBC
 * meta-data is usually not a fatal problem.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MetaDataAccessException extends NestedCheckedException {

  /**
   * Constructor for MetaDataAccessException.
   *
   * @param msg the detail message
   */
  public MetaDataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for MetaDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public MetaDataAccessException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
