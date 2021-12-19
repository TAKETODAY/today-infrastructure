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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of RowCallbackHandler. Convenient superclass for callback handlers.
 * An instance can only be used once.
 *
 * <p>We can either use this on its own (for example, in a test case, to ensure
 * that our result sets have valid dimensions), or use it as a superclass
 * for callback handlers that actually do something, and will benefit
 * from the dimension information it provides.
 *
 * <p>A usage example with JdbcTemplate:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 *
 * RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
 * jdbcTemplate.query("select * from user", countCallback);
 * int rowCount = countCallback.getRowCount();</pre>
 *
 * @author Rod Johnson
 * @since 4.0
 */
public class RowCountCallbackHandler implements RowCallbackHandler {

  /** Rows we've seen so far. */
  private int rowCount;

  /** Columns we've seen so far. */
  private int columnCount;

  /**
   * Indexed from 0. Type (as in java.sql.Types) for the columns
   * as returned by ResultSetMetaData object.
   */
  @Nullable
  private int[] columnTypes;

  /**
   * Indexed from 0. Column name as returned by ResultSetMetaData object.
   */
  @Nullable
  private String[] columnNames;

  /**
   * Implementation of ResultSetCallbackHandler.
   * Work out column size if this is the first row, otherwise just count rows.
   * <p>Subclasses can perform custom extraction or processing
   * by overriding the {@code processRow(ResultSet, int)} method.
   *
   * @see #processRow(ResultSet, int)
   */
  @Override
  public final void processRow(ResultSet rs) throws SQLException {
    if (this.rowCount == 0) {
      ResultSetMetaData rsmd = rs.getMetaData();
      this.columnCount = rsmd.getColumnCount();
      this.columnTypes = new int[this.columnCount];
      this.columnNames = new String[this.columnCount];
      for (int i = 0; i < this.columnCount; i++) {
        this.columnTypes[i] = rsmd.getColumnType(i + 1);
        this.columnNames[i] = JdbcUtils.lookupColumnName(rsmd, i + 1);
      }
      // could also get column names
    }
    processRow(rs, this.rowCount++);
  }

  /**
   * Subclasses may override this to perform custom extraction
   * or processing. This class's implementation does nothing.
   *
   * @param rs the ResultSet to extract data from. This method is
   * invoked for each row
   * @param rowNum number of the current row (starting from 0)
   */
  protected void processRow(ResultSet rs, int rowNum) throws SQLException {
  }

  /**
   * Return the types of the columns as java.sql.Types constants
   * Valid after processRow is invoked the first time.
   *
   * @return the types of the columns as java.sql.Types constants.
   * <b>Indexed from 0 to n-1.</b>
   */
  @Nullable
  public final int[] getColumnTypes() {
    return this.columnTypes;
  }

  /**
   * Return the names of the columns.
   * Valid after processRow is invoked the first time.
   *
   * @return the names of the columns.
   * <b>Indexed from 0 to n-1.</b>
   */
  @Nullable
  public final String[] getColumnNames() {
    return this.columnNames;
  }

  /**
   * Return the row count of this ResultSet.
   * Only valid after processing is complete
   *
   * @return the number of rows in this ResultSet
   */
  public final int getRowCount() {
    return this.rowCount;
  }

  /**
   * Return the number of columns in this result set.
   * Valid once we've seen the first row,
   * so subclasses can use it during processing
   *
   * @return the number of columns in this result set
   */
  public final int getColumnCount() {
    return this.columnCount;
  }

}
