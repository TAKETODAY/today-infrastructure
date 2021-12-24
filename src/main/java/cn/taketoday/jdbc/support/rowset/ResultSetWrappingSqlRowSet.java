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

package cn.taketoday.jdbc.support.rowset;

import java.io.Serial;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.jdbc.InvalidResultSetAccessException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * The default implementation of Framework's {@link SqlRowSet} interface, wrapping a
 * {@link ResultSet}, catching any {@link SQLException SQLExceptions} and
 * translating them to a corresponding Framework {@link InvalidResultSetAccessException}.
 *
 * <p>The passed-in ResultSet should already be disconnected if the SqlRowSet is supposed
 * to be usable in a disconnected fashion. This means that you will usually pass in a
 * {@code javax.sql.rowset.CachedRowSet}, which implements the ResultSet interface.
 *
 * <p>Note: Since JDBC 4.0, it has been clarified that any methods using a String to identify
 * the column should be using the column label. The column label is assigned using the ALIAS
 * keyword in the SQL query string. When the query doesn't use an ALIAS, the default label is
 * the column name. Most JDBC ResultSet implementations follow this new pattern but there are
 * exceptions such as the {@code com.sun.rowset.CachedRowSetImpl} class which only uses
 * the column name, ignoring any column labels. ResultSetWrappingSqlRowSet
 * will translate column labels to the correct column index to provide better support for the
 * {@code com.sun.rowset.CachedRowSetImpl} which is the default implementation used by
 * {@link cn.taketoday.jdbc.core.JdbcTemplate} when working with RowSets.
 *
 * <p>Note: This class implements the {@code java.io.Serializable} marker interface
 * through the SqlRowSet interface, but is only actually serializable if the disconnected
 * ResultSet/RowSet contained in it is serializable. Most CachedRowSet implementations
 * are actually serializable, so this should usually work out.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see ResultSet
 * @see javax.sql.rowset.CachedRowSet
 * @see cn.taketoday.jdbc.core.JdbcTemplate#queryForRowSet
 * @since 4.0
 */
public class ResultSetWrappingSqlRowSet implements SqlRowSet {

  @Serial
  private static final long serialVersionUID = -4688694393146734764L;

  private final ResultSet resultSet;

  private final SqlRowSetMetaData rowSetMetaData;

  private final Map<String, Integer> columnLabelMap;

  /**
   * Create a new ResultSetWrappingSqlRowSet for the given ResultSet.
   *
   * @param resultSet a disconnected ResultSet to wrap
   * (usually a {@code javax.sql.rowset.CachedRowSet})
   * @throws InvalidResultSetAccessException if extracting
   * the ResultSetMetaData failed
   * @see javax.sql.rowset.CachedRowSet
   * @see ResultSet#getMetaData
   * @see ResultSetWrappingSqlRowSetMetaData
   */
  public ResultSetWrappingSqlRowSet(ResultSet resultSet) throws InvalidResultSetAccessException {
    this.resultSet = resultSet;
    try {
      this.rowSetMetaData = new ResultSetWrappingSqlRowSetMetaData(resultSet.getMetaData());
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
    try {
      ResultSetMetaData rsmd = resultSet.getMetaData();
      if (rsmd != null) {
        int columnCount = rsmd.getColumnCount();
        this.columnLabelMap = CollectionUtils.newHashMap(columnCount);
        for (int i = 1; i <= columnCount; i++) {
          String key = rsmd.getColumnLabel(i);
          // Make sure to preserve first matching column for any given name,
          // as defined in ResultSet's type-level javadoc (lines 81 to 83).
          if (!this.columnLabelMap.containsKey(key)) {
            this.columnLabelMap.put(key, i);
          }
        }
      }
      else {
        this.columnLabelMap = Collections.emptyMap();
      }
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }

  }

  /**
   * Return the underlying ResultSet
   * (usually a {@code javax.sql.rowset.CachedRowSet}).
   *
   * @see javax.sql.rowset.CachedRowSet
   */
  public final ResultSet getResultSet() {
    return this.resultSet;
  }

  /**
   * @see ResultSetMetaData#getCatalogName(int)
   */
  @Override
  public final SqlRowSetMetaData getMetaData() {
    return this.rowSetMetaData;
  }

  /**
   * @see ResultSet#findColumn(String)
   */
  @Override
  public int findColumn(String columnLabel) throws InvalidResultSetAccessException {
    Integer columnIndex = this.columnLabelMap.get(columnLabel);
    if (columnIndex != null) {
      return columnIndex;
    }
    else {
      try {
        return this.resultSet.findColumn(columnLabel);
      }
      catch (SQLException se) {
        throw new InvalidResultSetAccessException(se);
      }
    }
  }

  // RowSet methods for extracting data values

  /**
   * @see ResultSet#getBigDecimal(int)
   */
  @Override
  @Nullable
  public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getBigDecimal(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getBigDecimal(String)
   */
  @Override
  @Nullable
  public BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException {
    return getBigDecimal(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getBoolean(int)
   */
  @Override
  public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getBoolean(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getBoolean(String)
   */
  @Override
  public boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException {
    return getBoolean(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getByte(int)
   */
  @Override
  public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getByte(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getByte(String)
   */
  @Override
  public byte getByte(String columnLabel) throws InvalidResultSetAccessException {
    return getByte(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getDate(int)
   */
  @Override
  @Nullable
  public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getDate(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getDate(String)
   */
  @Override
  @Nullable
  public Date getDate(String columnLabel) throws InvalidResultSetAccessException {
    return getDate(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getDate(int, Calendar)
   */
  @Override
  @Nullable
  public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getDate(columnIndex, cal);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getDate(String, Calendar)
   */
  @Override
  @Nullable
  public Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
    return getDate(findColumn(columnLabel), cal);
  }

  /**
   * @see ResultSet#getDouble(int)
   */
  @Override
  public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getDouble(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getDouble(String)
   */
  @Override
  public double getDouble(String columnLabel) throws InvalidResultSetAccessException {
    return getDouble(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getFloat(int)
   */
  @Override
  public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getFloat(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getFloat(String)
   */
  @Override
  public float getFloat(String columnLabel) throws InvalidResultSetAccessException {
    return getFloat(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getInt(int)
   */
  @Override
  public int getInt(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getInt(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getInt(String)
   */
  @Override
  public int getInt(String columnLabel) throws InvalidResultSetAccessException {
    return getInt(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getLong(int)
   */
  @Override
  public long getLong(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getLong(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getLong(String)
   */
  @Override
  public long getLong(String columnLabel) throws InvalidResultSetAccessException {
    return getLong(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getNString(int)
   */
  @Override
  @Nullable
  public String getNString(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getNString(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getNString(String)
   */
  @Override
  @Nullable
  public String getNString(String columnLabel) throws InvalidResultSetAccessException {
    return getNString(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getObject(int)
   */
  @Override
  @Nullable
  public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getObject(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getObject(String)
   */
  @Override
  @Nullable
  public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
    return getObject(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getObject(int, Map)
   */
  @Override
  @Nullable
  public Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getObject(columnIndex, map);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getObject(String, Map)
   */
  @Override
  @Nullable
  public Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
    return getObject(findColumn(columnLabel), map);
  }

  /**
   * @see ResultSet#getObject(int, Class)
   */
  @Override
  @Nullable
  public <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getObject(columnIndex, type);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getObject(String, Class)
   */
  @Override
  @Nullable
  public <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException {
    return getObject(findColumn(columnLabel), type);
  }

  /**
   * @see ResultSet#getShort(int)
   */
  @Override
  public short getShort(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getShort(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getShort(String)
   */
  @Override
  public short getShort(String columnLabel) throws InvalidResultSetAccessException {
    return getShort(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getString(int)
   */
  @Override
  @Nullable
  public String getString(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getString(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getString(String)
   */
  @Override
  @Nullable
  public String getString(String columnLabel) throws InvalidResultSetAccessException {
    return getString(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getTime(int)
   */
  @Override
  @Nullable
  public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getTime(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getTime(String)
   */
  @Override
  @Nullable
  public Time getTime(String columnLabel) throws InvalidResultSetAccessException {
    return getTime(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getTime(int, Calendar)
   */
  @Override
  @Nullable
  public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getTime(columnIndex, cal);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getTime(String, Calendar)
   */
  @Override
  @Nullable
  public Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
    return getTime(findColumn(columnLabel), cal);
  }

  /**
   * @see ResultSet#getTimestamp(int)
   */
  @Override
  @Nullable
  public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getTimestamp(columnIndex);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getTimestamp(String)
   */
  @Override
  @Nullable
  public Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException {
    return getTimestamp(findColumn(columnLabel));
  }

  /**
   * @see ResultSet#getTimestamp(int, Calendar)
   */
  @Override
  @Nullable
  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getTimestamp(columnIndex, cal);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getTimestamp(String, Calendar)
   */
  @Override
  @Nullable
  public Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
    return getTimestamp(findColumn(columnLabel), cal);
  }

  // RowSet navigation methods

  /**
   * @see ResultSet#absolute(int)
   */
  @Override
  public boolean absolute(int row) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.absolute(row);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#afterLast()
   */
  @Override
  public void afterLast() throws InvalidResultSetAccessException {
    try {
      this.resultSet.afterLast();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#beforeFirst()
   */
  @Override
  public void beforeFirst() throws InvalidResultSetAccessException {
    try {
      this.resultSet.beforeFirst();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#first()
   */
  @Override
  public boolean first() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.first();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#getRow()
   */
  @Override
  public int getRow() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.getRow();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#isAfterLast()
   */
  @Override
  public boolean isAfterLast() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.isAfterLast();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#isBeforeFirst()
   */
  @Override
  public boolean isBeforeFirst() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.isBeforeFirst();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#isFirst()
   */
  @Override
  public boolean isFirst() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.isFirst();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#isLast()
   */
  @Override
  public boolean isLast() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.isLast();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#last()
   */
  @Override
  public boolean last() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.last();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#next()
   */
  @Override
  public boolean next() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.next();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#previous()
   */
  @Override
  public boolean previous() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.previous();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#relative(int)
   */
  @Override
  public boolean relative(int rows) throws InvalidResultSetAccessException {
    try {
      return this.resultSet.relative(rows);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  /**
   * @see ResultSet#wasNull()
   */
  @Override
  public boolean wasNull() throws InvalidResultSetAccessException {
    try {
      return this.resultSet.wasNull();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

}
