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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An interface used by {@link JdbcTemplate} for processing rows of a
 * {@link ResultSet} on a per-row basis. Implementations of
 * this interface perform the actual work of processing each row
 * but don't need to worry about exception handling.
 * {@link SQLException SQLExceptions} will be caught and handled
 * by the calling JdbcTemplate.
 *
 * <p>In contrast to a {@link ResultSetExtractor}, a RowCallbackHandler
 * object is typically stateful: It keeps the result state within the
 * object, to be available for later inspection. See
 * {@link RowCountCallbackHandler} for a usage example.
 *
 * <p>Consider using a {@link RowMapper} instead if you need to map
 * exactly one result object per row, assembling them into a List.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 * @see RowMapper
 * @see ResultSetExtractor
 * @see RowCountCallbackHandler
 */
@FunctionalInterface
public interface RowCallbackHandler {

  /**
   * Implementations must implement this method to process each row of data
   * in the ResultSet. This method should not call {@code next()} on
   * the ResultSet; it is only supposed to extract values of the current row.
   * <p>Exactly what the implementation chooses to do is up to it:
   * A trivial implementation might simply count rows, while another
   * implementation might build an XML document.
   *
   * @param rs the ResultSet to process (pre-initialized for the current row)
   * @throws SQLException if an SQLException is encountered getting
   * column values (that is, there's no need to catch SQLException)
   */
  void processRow(ResultSet rs) throws SQLException;

}
