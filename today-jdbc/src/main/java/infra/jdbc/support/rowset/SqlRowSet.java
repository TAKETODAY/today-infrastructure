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

package infra.jdbc.support.rowset;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import infra.jdbc.InvalidResultSetAccessException;
import infra.lang.Nullable;
import infra.jdbc.core.JdbcTemplate;

/**
 * Mirror interface for {@link javax.sql.RowSet}, representing a disconnected variant of
 * {@link java.sql.ResultSet} data.
 *
 * <p>The main difference to the standard JDBC RowSet is that a {@link java.sql.SQLException}
 * is never thrown here. This allows an SqlRowSet to be used without having to deal with
 * checked exceptions. An SqlRowSet will throw Framework's {@link InvalidResultSetAccessException}
 * instead (when appropriate).
 *
 * <p>Note: This interface extends the {@code java.io.Serializable} marker interface.
 * Implementations, which typically hold disconnected data, are encouraged to be actually
 * serializable (as far as possible).
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see javax.sql.RowSet
 * @see java.sql.ResultSet
 * @see InvalidResultSetAccessException
 * @see JdbcTemplate#queryForRowSet
 * @since 4.0
 */
public interface SqlRowSet extends Serializable {

  /**
   * Retrieve the meta-data, i.e. number, types and properties
   * for the columns of this row set.
   *
   * @return a corresponding SqlRowSetMetaData instance
   * @see java.sql.ResultSet#getMetaData()
   */
  SqlRowSetMetaData getMetaData();

  /**
   * Map the given column label to its column index.
   *
   * @param columnLabel the name of the column
   * @return the column index for the given column label
   * @see java.sql.ResultSet#findColumn(String)
   */
  int findColumn(String columnLabel) throws InvalidResultSetAccessException;

  // RowSet methods for extracting data values

  /**
   * Retrieve the value of the indicated column in the current row as a BigDecimal object.
   *
   * @param columnIndex the column index
   * @return an BigDecimal object representing the column value
   * @see java.sql.ResultSet#getBigDecimal(int)
   */
  @Nullable
  BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a BigDecimal object.
   *
   * @param columnLabel the column label
   * @return an BigDecimal object representing the column value
   * @see java.sql.ResultSet#getBigDecimal(String)
   */
  @Nullable
  BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a boolean.
   *
   * @param columnIndex the column index
   * @return a boolean representing the column value
   * @see java.sql.ResultSet#getBoolean(int)
   */
  boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a boolean.
   *
   * @param columnLabel the column label
   * @return a boolean representing the column value
   * @see java.sql.ResultSet#getBoolean(String)
   */
  boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a byte.
   *
   * @param columnIndex the column index
   * @return a byte representing the column value
   * @see java.sql.ResultSet#getByte(int)
   */
  byte getByte(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a byte.
   *
   * @param columnLabel the column label
   * @return a byte representing the column value
   * @see java.sql.ResultSet#getByte(String)
   */
  byte getByte(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Date object.
   *
   * @param columnIndex the column index
   * @return a Date object representing the column value
   * @see java.sql.ResultSet#getDate(int)
   */
  @Nullable
  Date getDate(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Date object.
   *
   * @param columnLabel the column label
   * @return a Date object representing the column value
   * @see java.sql.ResultSet#getDate(String)
   */
  @Nullable
  Date getDate(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Date object.
   *
   * @param columnIndex the column index
   * @param cal the Calendar to use in constructing the Date
   * @return a Date object representing the column value
   * @see java.sql.ResultSet#getDate(int, Calendar)
   */
  @Nullable
  Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Date object.
   *
   * @param columnLabel the column label
   * @param cal the Calendar to use in constructing the Date
   * @return a Date object representing the column value
   * @see java.sql.ResultSet#getDate(String, Calendar)
   */
  @Nullable
  Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Double object.
   *
   * @param columnIndex the column index
   * @return a Double object representing the column value
   * @see java.sql.ResultSet#getDouble(int)
   */
  double getDouble(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Double object.
   *
   * @param columnLabel the column label
   * @return a Double object representing the column value
   * @see java.sql.ResultSet#getDouble(String)
   */
  double getDouble(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a float.
   *
   * @param columnIndex the column index
   * @return a float representing the column value
   * @see java.sql.ResultSet#getFloat(int)
   */
  float getFloat(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a float.
   *
   * @param columnLabel the column label
   * @return a float representing the column value
   * @see java.sql.ResultSet#getFloat(String)
   */
  float getFloat(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an int.
   *
   * @param columnIndex the column index
   * @return an int representing the column value
   * @see java.sql.ResultSet#getInt(int)
   */
  int getInt(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an int.
   *
   * @param columnLabel the column label
   * @return an int representing the column value
   * @see java.sql.ResultSet#getInt(String)
   */
  int getInt(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a long.
   *
   * @param columnIndex the column index
   * @return a long representing the column value
   * @see java.sql.ResultSet#getLong(int)
   */
  long getLong(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a long.
   *
   * @param columnLabel the column label
   * @return a long representing the column value
   * @see java.sql.ResultSet#getLong(String)
   */
  long getLong(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a String
   * (for NCHAR, NVARCHAR, LONGNVARCHAR columns).
   *
   * @param columnIndex the column index
   * @return a String representing the column value
   * @see java.sql.ResultSet#getNString(int)
   * @since 4.0
   */
  @Nullable
  String getNString(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a String
   * (for NCHAR, NVARCHAR, LONGNVARCHAR columns).
   *
   * @param columnLabel the column label
   * @return a String representing the column value
   * @see java.sql.ResultSet#getNString(String)
   * @since 4.0
   */
  @Nullable
  String getNString(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an Object.
   *
   * @param columnIndex the column index
   * @return a Object representing the column value
   * @see java.sql.ResultSet#getObject(int)
   */
  @Nullable
  Object getObject(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an Object.
   *
   * @param columnLabel the column label
   * @return a Object representing the column value
   * @see java.sql.ResultSet#getObject(String)
   */
  @Nullable
  Object getObject(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an Object.
   *
   * @param columnIndex the column index
   * @param map a Map object containing the mapping from SQL types to Java types
   * @return a Object representing the column value
   * @see java.sql.ResultSet#getObject(int, Map)
   */
  @Nullable
  Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an Object.
   *
   * @param columnLabel the column label
   * @param map a Map object containing the mapping from SQL types to Java types
   * @return a Object representing the column value
   * @see java.sql.ResultSet#getObject(String, Map)
   */
  @Nullable
  Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an Object.
   *
   * @param columnIndex the column index
   * @param type the Java type to convert the designated column to
   * @return a Object representing the column value
   * @see java.sql.ResultSet#getObject(int, Class)
   * @since 4.0
   */
  @Nullable
  <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as an Object.
   *
   * @param columnLabel the column label
   * @param type the Java type to convert the designated column to
   * @return a Object representing the column value
   * @see java.sql.ResultSet#getObject(String, Class)
   * @since 4.0
   */
  @Nullable
  <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a short.
   *
   * @param columnIndex the column index
   * @return a short representing the column value
   * @see java.sql.ResultSet#getShort(int)
   */
  short getShort(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a short.
   *
   * @param columnLabel the column label
   * @return a short representing the column value
   * @see java.sql.ResultSet#getShort(String)
   */
  short getShort(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a String.
   *
   * @param columnIndex the column index
   * @return a String representing the column value
   * @see java.sql.ResultSet#getString(int)
   */
  @Nullable
  String getString(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a String.
   *
   * @param columnLabel the column label
   * @return a String representing the column value
   * @see java.sql.ResultSet#getString(String)
   */
  @Nullable
  String getString(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Time object.
   *
   * @param columnIndex the column index
   * @return a Time object representing the column value
   * @see java.sql.ResultSet#getTime(int)
   */
  @Nullable
  Time getTime(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Time object.
   *
   * @param columnLabel the column label
   * @return a Time object representing the column value
   * @see java.sql.ResultSet#getTime(String)
   */
  @Nullable
  Time getTime(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Time object.
   *
   * @param columnIndex the column index
   * @param cal the Calendar to use in constructing the Date
   * @return a Time object representing the column value
   * @see java.sql.ResultSet#getTime(int, Calendar)
   */
  @Nullable
  Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Time object.
   *
   * @param columnLabel the column label
   * @param cal the Calendar to use in constructing the Date
   * @return a Time object representing the column value
   * @see java.sql.ResultSet#getTime(String, Calendar)
   */
  @Nullable
  Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Timestamp object.
   *
   * @param columnIndex the column index
   * @return a Timestamp object representing the column value
   * @see java.sql.ResultSet#getTimestamp(int)
   */
  @Nullable
  Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Timestamp object.
   *
   * @param columnLabel the column label
   * @return a Timestamp object representing the column value
   * @see java.sql.ResultSet#getTimestamp(String)
   */
  @Nullable
  Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Timestamp object.
   *
   * @param columnIndex the column index
   * @param cal the Calendar to use in constructing the Date
   * @return a Timestamp object representing the column value
   * @see java.sql.ResultSet#getTimestamp(int, Calendar)
   */
  @Nullable
  Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

  /**
   * Retrieve the value of the indicated column in the current row as a Timestamp object.
   *
   * @param columnLabel the column label
   * @param cal the Calendar to use in constructing the Date
   * @return a Timestamp object representing the column value
   * @see java.sql.ResultSet#getTimestamp(String, Calendar)
   */
  @Nullable
  Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

  // RowSet navigation methods

  /**
   * Move the cursor to the given row number in the row set, just after the last row.
   *
   * @param row the number of the row where the cursor should move
   * @return {@code true} if the cursor is on the row set, {@code false} otherwise
   * @see java.sql.ResultSet#absolute(int)
   */
  boolean absolute(int row) throws InvalidResultSetAccessException;

  /**
   * Move the cursor to the end of this row set.
   *
   * @see java.sql.ResultSet#afterLast()
   */
  void afterLast() throws InvalidResultSetAccessException;

  /**
   * Move the cursor to the front of this row set, just before the first row.
   *
   * @see java.sql.ResultSet#beforeFirst()
   */
  void beforeFirst() throws InvalidResultSetAccessException;

  /**
   * Move the cursor to the first row of this row set.
   *
   * @return {@code true} if the cursor is on a valid row, {@code false} otherwise
   * @see java.sql.ResultSet#first()
   */
  boolean first() throws InvalidResultSetAccessException;

  /**
   * Retrieve the current row number.
   *
   * @return the current row number
   * @see java.sql.ResultSet#getRow()
   */
  int getRow() throws InvalidResultSetAccessException;

  /**
   * Retrieve whether the cursor is after the last row of this row set.
   *
   * @return {@code true} if the cursor is after the last row, {@code false} otherwise
   * @see java.sql.ResultSet#isAfterLast()
   */
  boolean isAfterLast() throws InvalidResultSetAccessException;

  /**
   * Retrieve whether the cursor is before the first row of this row set.
   *
   * @return {@code true} if the cursor is before the first row, {@code false} otherwise
   * @see java.sql.ResultSet#isBeforeFirst()
   */
  boolean isBeforeFirst() throws InvalidResultSetAccessException;

  /**
   * Retrieve whether the cursor is on the first row of this row set.
   *
   * @return {@code true} if the cursor is after the first row, {@code false} otherwise
   * @see java.sql.ResultSet#isFirst()
   */
  boolean isFirst() throws InvalidResultSetAccessException;

  /**
   * Retrieve whether the cursor is on the last row of this row set.
   *
   * @return {@code true} if the cursor is after the last row, {@code false} otherwise
   * @see java.sql.ResultSet#isLast()
   */
  boolean isLast() throws InvalidResultSetAccessException;

  /**
   * Move the cursor to the last row of this row set.
   *
   * @return {@code true} if the cursor is on a valid row, {@code false} otherwise
   * @see java.sql.ResultSet#last()
   */
  boolean last() throws InvalidResultSetAccessException;

  /**
   * Move the cursor to the next row.
   *
   * @return {@code true} if the new row is valid, {@code false} if there are no more rows
   * @see java.sql.ResultSet#next()
   */
  boolean next() throws InvalidResultSetAccessException;

  /**
   * Move the cursor to the previous row.
   *
   * @return {@code true} if the new row is valid, {@code false} if it is off the row set
   * @see java.sql.ResultSet#previous()
   */
  boolean previous() throws InvalidResultSetAccessException;

  /**
   * Move the cursor a relative number of rows, either positive or negative.
   *
   * @return {@code true} if the cursor is on a row, {@code false} otherwise
   * @see java.sql.ResultSet#relative(int)
   */
  boolean relative(int rows) throws InvalidResultSetAccessException;

  /**
   * Report whether the last column read had a value of SQL {@code NULL}.
   * <p>Note that you must first call one of the getter methods and then
   * call the {@code wasNull()} method.
   *
   * @return {@code true} if the most recent column retrieved was
   * SQL {@code NULL}, {@code false} otherwise
   * @see java.sql.ResultSet#wasNull()
   */
  boolean wasNull() throws InvalidResultSetAccessException;

}
