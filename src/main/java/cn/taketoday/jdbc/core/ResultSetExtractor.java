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

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Callback interface used by {@link JdbcTemplate}'s query methods.
 * Implementations of this interface perform the actual work of extracting
 * results from a {@link ResultSet}, but don't need to worry
 * about exception handling. {@link SQLException SQLExceptions}
 * will be caught and handled by the calling JdbcTemplate.
 *
 * <p>This interface is mainly used within the JDBC framework itself.
 * A {@link RowMapper} is usually a simpler choice for ResultSet processing,
 * mapping one result object per row instead of one result object for
 * the entire ResultSet.
 *
 * <p>Note: In contrast to a {@link RowCallbackHandler}, a ResultSetExtractor
 * object is typically stateless and thus reusable, as long as it doesn't
 * access stateful resources (such as output streams when streaming LOB
 * contents) or keep result state within the object.
 *
 * @param <T> the result type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see RowMapper
 * @see cn.taketoday.jdbc.core.support.AbstractLobStreamingResultSetExtractor
 * @since April 24, 2003
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {

  /**
   * Implementations must implement this method to process the entire ResultSet.
   *
   * @param rs the ResultSet to extract data from. Implementations should
   * not close this: it will be closed by the calling JdbcTemplate.
   * @return an arbitrary result object, or {@code null} if none
   * (the extractor will typically be stateful in the latter case).
   * @throws SQLException if an SQLException is encountered getting column
   * values or navigating (that is, there's no need to catch SQLException)
   * @throws DataAccessException in case of custom exceptions
   */
  @Nullable
  T extractData(ResultSet rs) throws SQLException, DataAccessException;

}
