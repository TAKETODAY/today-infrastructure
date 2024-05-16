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

package cn.taketoday.jdbc.datasource.lookup;

import cn.taketoday.dao.NonTransientDataAccessException;

/**
 * Exception to be thrown by a DataSourceLookup implementation,
 * indicating that the specified DataSource could not be obtained.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DataSourceLookupFailureException extends NonTransientDataAccessException {

  /**
   * Constructor for DataSourceLookupFailureException.
   *
   * @param msg the detail message
   */
  public DataSourceLookupFailureException(String msg) {
    super(msg);
  }

  /**
   * Constructor for DataSourceLookupFailureException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using a underlying
   * lookup API such as JNDI)
   */
  public DataSourceLookupFailureException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
