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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.jdbc.InvalidResultSetAccessException;
import cn.taketoday.lang.Nullable;

/**
 * The default implementation of Framework's {@link SqlRowSetMetaData} interface, wrapping a
 * {@link ResultSetMetaData} instance, catching any {@link SQLException SQLExceptions}
 * and translating them to a corresponding Framework {@link InvalidResultSetAccessException}.
 *
 * <p>Used by {@link ResultSetWrappingSqlRowSet}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see ResultSetWrappingSqlRowSet#getMetaData()
 * @since 4.0
 */
public class ResultSetWrappingSqlRowSetMetaData implements SqlRowSetMetaData {

  private final ResultSetMetaData resultSetMetaData;

  @Nullable
  private String[] columnNames;

  /**
   * Create a new ResultSetWrappingSqlRowSetMetaData object
   * for the given ResultSetMetaData instance.
   *
   * @param resultSetMetaData a disconnected ResultSetMetaData instance
   * to wrap (usually a {@code javax.sql.RowSetMetaData} instance)
   * @see java.sql.ResultSet#getMetaData
   * @see javax.sql.RowSetMetaData
   * @see ResultSetWrappingSqlRowSet#getMetaData
   */
  public ResultSetWrappingSqlRowSetMetaData(ResultSetMetaData resultSetMetaData) {
    this.resultSetMetaData = resultSetMetaData;
  }

  @Override
  public String getCatalogName(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getCatalogName(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String getColumnClassName(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnClassName(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public int getColumnCount() throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnCount();
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String[] getColumnNames() throws InvalidResultSetAccessException {
    if (this.columnNames == null) {
      this.columnNames = new String[getColumnCount()];
      for (int i = 0; i < getColumnCount(); i++) {
        this.columnNames[i] = getColumnName(i + 1);
      }
    }
    return this.columnNames;
  }

  @Override
  public int getColumnDisplaySize(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnDisplaySize(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String getColumnLabel(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnLabel(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String getColumnName(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnName(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public int getColumnType(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnType(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String getColumnTypeName(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getColumnTypeName(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public int getPrecision(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getPrecision(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public int getScale(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getScale(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String getSchemaName(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getSchemaName(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public String getTableName(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.getTableName(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public boolean isCaseSensitive(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.isCaseSensitive(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public boolean isCurrency(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.isCurrency(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

  @Override
  public boolean isSigned(int column) throws InvalidResultSetAccessException {
    try {
      return this.resultSetMetaData.isSigned(column);
    }
    catch (SQLException se) {
      throw new InvalidResultSetAccessException(se);
    }
  }

}
