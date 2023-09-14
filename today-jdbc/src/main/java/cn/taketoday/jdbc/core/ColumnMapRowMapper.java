/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedCaseInsensitiveMap;

/**
 * {@link RowMapper} implementation that creates a {@code java.util.Map}
 * for each row, representing all columns as key-value pairs: one
 * entry for each column, with the column name as key.
 *
 * <p>The Map implementation to use and the key to use for each column
 * in the column Map can be customized by overriding {@link #createColumnMap}
 * and {@link #getColumnKey}, respectively.
 *
 * <p><b>Note:</b> By default, {@code ColumnMapRowMapper} will try to build a linked Map
 * with case-insensitive keys, to preserve column order as well as allow any
 * casing to be used for column names.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcTemplate#queryForList(String)
 * @see JdbcTemplate#queryForMap(String)
 * @since 4.0
 */
public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {

  @Override
  public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    Map<String, Object> mapOfColumnValues = createColumnMap(columnCount);
    for (int i = 1; i <= columnCount; i++) {
      String column = JdbcUtils.lookupColumnName(rsmd, i);
      mapOfColumnValues.putIfAbsent(getColumnKey(column), getColumnValue(rs, i));
    }
    return mapOfColumnValues;
  }

  /**
   * Create a Map instance to be used as column map.
   * <p>By default, a linked case-insensitive Map will be created.
   *
   * @param columnCount the column count, to be used as initial
   * capacity for the Map
   * @return the new Map instance
   * @see cn.taketoday.util.LinkedCaseInsensitiveMap
   */
  protected Map<String, Object> createColumnMap(int columnCount) {
    return new LinkedCaseInsensitiveMap<>(columnCount);
  }

  /**
   * Determine the key to use for the given column in the column Map.
   * <p>By default, the supplied column name will be returned unmodified.
   *
   * @param columnName the column name as returned by the ResultSet
   * @return the column key to use
   * @see ResultSetMetaData#getColumnName
   */
  protected String getColumnKey(String columnName) {
    return columnName;
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>The default implementation uses the {@code getObject} method.
   * Additionally, this implementation includes a "hack" to get around Oracle
   * returning a non standard object for their TIMESTAMP data type.
   *
   * @param rs the ResultSet holding the data
   * @param index the column index
   * @return the Object returned
   * @see cn.taketoday.jdbc.support.JdbcUtils#getResultSetValue
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index);
  }

}
