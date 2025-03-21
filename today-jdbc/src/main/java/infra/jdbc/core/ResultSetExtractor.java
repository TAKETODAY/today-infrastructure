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

import java.sql.ResultSet;
import java.sql.SQLException;

import infra.dao.DataAccessException;
import infra.lang.Nullable;
import infra.jdbc.core.support.AbstractLobStreamingResultSetExtractor;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see RowMapper
 * @see AbstractLobStreamingResultSetExtractor
 * @since 4.0
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

  // static

  static <T> RowMapperResultSetExtractor<T> forRowMapper(RowMapper<T> mapper, int rowsExpected) {
    return new RowMapperResultSetExtractor<>(mapper, rowsExpected);
  }

  static <T> RowMapperResultSetExtractor<T> forRowMapper(RowMapper<T> mapper) {
    return new RowMapperResultSetExtractor<>(mapper);
  }

  static SqlRowSetResultSetExtractor forSqlRowSet() {
    return new SqlRowSetResultSetExtractor();
  }

}
