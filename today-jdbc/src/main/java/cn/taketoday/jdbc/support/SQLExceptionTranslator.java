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

import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for translating between {@link SQLException SQLExceptions}
 * and Framework's data access strategy-agnostic {@link DataAccessException}
 * hierarchy.
 *
 * <p>Implementations can be generic (for example, using
 * {@link java.sql.SQLException#getSQLState() SQLState} codes for JDBC) or wholly
 * proprietary (for example, using Oracle error codes) for greater precision.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see cn.taketoday.dao.DataAccessException
 */
@FunctionalInterface
public interface SQLExceptionTranslator {

  /**
   * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
   * <p>The returned DataAccessException is supposed to contain the original
   * {@code SQLException} as root cause. However, client code may not generally
   * rely on this due to DataAccessExceptions possibly being caused by other resource
   * APIs as well. That said, a {@code getRootCause() instanceof SQLException}
   * check (and subsequent cast) is considered reliable when expecting JDBC-based
   * access to have happened.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (if known)
   * @param ex the offending {@code SQLException}
   * @return the DataAccessException wrapping the {@code SQLException},
   * or {@code null} if no specific translation could be applied
   * @see cn.taketoday.dao.DataAccessException#getRootCause()
   */
  @Nullable
  DataAccessException translate(String task, @Nullable String sql, SQLException ex);

}
