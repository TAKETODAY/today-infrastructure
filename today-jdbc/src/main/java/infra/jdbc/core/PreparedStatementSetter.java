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

package infra.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * General callback interface used by the {@link JdbcTemplate} class.
 *
 * <p>This interface sets values on a {@link PreparedStatement} provided
 * by the JdbcTemplate class, for each of a number of updates in a batch using the
 * same SQL. Implementations are responsible for setting any necessary parameters.
 * SQL with placeholders will already have been supplied.
 *
 * <p>It's easier to use this interface than {@link PreparedStatementCreator}:
 * The JdbcTemplate will create the PreparedStatement, with the callback
 * only being responsible for setting parameter values.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * SQLExceptions that may be thrown from operations they attempt.
 * The JdbcTemplate class will catch and handle SQLExceptions appropriately.
 *
 * @author Rod Johnson
 * @see JdbcTemplate#update(String, PreparedStatementSetter)
 * @see JdbcTemplate#query(String, PreparedStatementSetter, ResultSetExtractor)
 * @since March 2, 2003
 */
@FunctionalInterface
public interface PreparedStatementSetter {

  /**
   * Set parameter values on the given PreparedStatement.
   *
   * @param ps the PreparedStatement to invoke setter methods on
   * @throws SQLException if an SQLException is encountered
   * (i.e. there is no need to catch SQLException)
   */
  void setValues(PreparedStatement ps) throws SQLException;

}
