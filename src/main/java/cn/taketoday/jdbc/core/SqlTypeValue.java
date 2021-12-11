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

package cn.taketoday.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented for setting values for more complex database-specific
 * types not supported by the standard {@code setObject} method. This is
 * effectively an extended variant of {@link cn.taketoday.jdbc.support.SqlValue}.
 *
 * <p>Implementations perform the actual work of setting the actual values. They must
 * implement the callback method {@code setTypeValue} which can throw SQLExceptions
 * that will be caught and translated by the calling code. This callback method has
 * access to the underlying Connection via the given PreparedStatement object, if that
 * should be needed to create any database-specific objects.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see java.sql.Types
 * @see PreparedStatement#setObject
 * @see JdbcOperations#update(String, Object[], int[])
 * @see cn.taketoday.jdbc.support.SqlValue
 * @since 4.0
 */
public interface SqlTypeValue {

  /**
   * Constant that indicates an unknown (or unspecified) SQL type.
   * Passed into {@code setTypeValue} if the original operation method
   * does not specify an SQL type.
   *
   * @see java.sql.Types
   * @see JdbcOperations#update(String, Object[])
   */
  int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;

  /**
   * Set the type value on the given PreparedStatement.
   *
   * @param ps the PreparedStatement to work on
   * @param paramIndex the index of the parameter for which we need to set the value
   * @param sqlType the SQL type of the parameter we are setting
   * @param typeName the type name of the parameter (optional)
   * @throws SQLException if an SQLException is encountered while setting parameter values
   * @see java.sql.Types
   * @see PreparedStatement#setObject
   */
  void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
          throws SQLException;

}
