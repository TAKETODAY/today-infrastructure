/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.jdbc.metadata;

import javax.sql.DataSource;

/**
 * Provides access meta-data that is commonly available from most pooled
 * {@link DataSource} implementations.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface DataSourcePoolMetadata {

  /**
   * Return the usage of the pool as value between 0 and 1 (or -1 if the pool is not
   * limited).
   * <ul>
   * <li>1 means that the maximum number of connections have been allocated</li>
   * <li>0 means that no connection is currently active</li>
   * <li>-1 means there is not limit to the number of connections that can be allocated
   * </li>
   * </ul>
   * This may also return {@code null} if the data source does not provide the necessary
   * information to compute the poll usage.
   *
   * @return the usage value or {@code null}
   */
  Float getUsage();

  /**
   * Return the current number of active connections that have been allocated from the
   * data source or {@code null} if that information is not available.
   *
   * @return the number of active connections or {@code null}
   */
  Integer getActive();

  /**
   * Return the number of established but idle connections. Can also return {@code null}
   * if that information is not available.
   *
   * @return the number of established but idle connections or {@code null}
   * @see #getActive()
   */
  default Integer getIdle() {
    return null;
  }

  /**
   * Return the maximum number of active connections that can be allocated at the same
   * time or {@code -1} if there is no limit. Can also return {@code null} if that
   * information is not available.
   *
   * @return the maximum number of active connections or {@code null}
   */
  Integer getMax();

  /**
   * Return the minimum number of idle connections in the pool or {@code null} if that
   * information is not available.
   *
   * @return the minimum number of active connections or {@code null}
   */
  Integer getMin();

  /**
   * Return the query to use to validate that a connection is valid or {@code null} if
   * that information is not available.
   *
   * @return the validation query or {@code null}
   */
  String getValidationQuery();

  /**
   * The default auto-commit state of connections created by this pool. If not set
   * ({@code null}), default is JDBC driver default (If set to null then the
   * java.sql.Connection.setAutoCommit(boolean) method will not be called.)
   *
   * @return the default auto-commit state or {@code null}
   */
  Boolean getDefaultAutoCommit();

}
