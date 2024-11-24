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

package infra.jdbc.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.jdbc.core.DisposableSqlTypeValue;
import infra.jdbc.core.SqlTypeValue;

/**
 * Simple interface for complex types to be set as statement parameters.
 *
 * <p>Implementations perform the actual work of setting the actual values. They must
 * implement the callback method {@code setValue} which can throw SQLExceptions
 * that will be caught and translated by the calling code. This callback method has
 * access to the underlying Connection via the given PreparedStatement object, if that
 * should be needed to create any database-specific objects.
 *
 * @author Juergen Hoeller
 * @see SqlTypeValue
 * @see DisposableSqlTypeValue
 * @since 4.0
 */
public interface SqlValue {

  /**
   * Set the value on the given PreparedStatement.
   *
   * @param ps the PreparedStatement to work on
   * @param paramIndex the index of the parameter for which we need to set the value
   * @throws SQLException if an SQLException is encountered while setting parameter values
   */
  void setValue(PreparedStatement ps, int paramIndex) throws SQLException;

  /**
   * Clean up resources held by this value object.
   */
  void cleanup();

}
