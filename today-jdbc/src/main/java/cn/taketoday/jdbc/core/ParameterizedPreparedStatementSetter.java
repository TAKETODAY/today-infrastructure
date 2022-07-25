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

/**
 * Parameterized callback interface used by the {@link JdbcTemplate} class for
 * batch updates.
 *
 * <p>This interface sets values on a {@link PreparedStatement} provided
 * by the JdbcTemplate class, for each of a number of updates in a batch using the
 * same SQL. Implementations are responsible for setting any necessary parameters.
 * SQL with placeholders will already have been supplied.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with SQLExceptions
 * that may be thrown from operations they attempt. The JdbcTemplate class will
 * catch and handle SQLExceptions appropriately.
 *
 * @param <T> the argument type
 * @author Nicolas Fabre
 * @author Thomas Risberg
 * @see JdbcTemplate#batchUpdate(String, java.util.Collection, int, ParameterizedPreparedStatementSetter)
 * @since 4.0
 */
@FunctionalInterface
public interface ParameterizedPreparedStatementSetter<T> {

  /**
   * Set parameter values on the given PreparedStatement.
   *
   * @param ps the PreparedStatement to invoke setter methods on
   * @param argument the object containing the values to be set
   * @throws SQLException if an SQLException is encountered (i.e. there is no need to catch SQLException)
   */
  void setValues(PreparedStatement ps, T argument) throws SQLException;

}
