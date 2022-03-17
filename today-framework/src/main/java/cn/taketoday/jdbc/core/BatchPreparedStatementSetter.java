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
 * Batch update callback interface used by the {@link JdbcTemplate} class.
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
 * @author Rod Johnson
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 * @see InterruptibleBatchPreparedStatementSetter
 * @since March 2, 2003
 */
public interface BatchPreparedStatementSetter {

  /**
   * Set parameter values on the given PreparedStatement.
   *
   * @param ps the PreparedStatement to invoke setter methods on
   * @param i index of the statement we're issuing in the batch, starting from 0
   * @throws SQLException if an SQLException is encountered
   * (i.e. there is no need to catch SQLException)
   */
  void setValues(PreparedStatement ps, int i) throws SQLException;

  /**
   * Return the size of the batch.
   *
   * @return the number of statements in the batch
   */
  int getBatchSize();

}
