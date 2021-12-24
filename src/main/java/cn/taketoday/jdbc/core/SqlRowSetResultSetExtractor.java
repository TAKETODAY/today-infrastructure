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

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import cn.taketoday.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import cn.taketoday.jdbc.support.rowset.SqlRowSet;

/**
 * {@link ResultSetExtractor} implementation that returns a Framework {@link SqlRowSet}
 * representation for each given {@link ResultSet}.
 *
 * <p>The default implementation uses a standard JDBC CachedRowSet underneath.
 *
 * @author Juergen Hoeller
 * @see #newCachedRowSet
 * @see cn.taketoday.jdbc.support.rowset.SqlRowSet
 * @see JdbcTemplate#queryForRowSet(String)
 * @see CachedRowSet
 * @since 4.0
 */
public class SqlRowSetResultSetExtractor implements ResultSetExtractor<SqlRowSet> {

  private static final RowSetFactory rowSetFactory;

  static {
    try {
      rowSetFactory = RowSetProvider.newFactory();
    }
    catch (SQLException ex) {
      throw new IllegalStateException("Cannot create RowSetFactory through RowSetProvider", ex);
    }
  }

  @Override
  public SqlRowSet extractData(ResultSet rs) throws SQLException {
    return createSqlRowSet(rs);
  }

  /**
   * Create a {@link SqlRowSet} that wraps the given {@link ResultSet},
   * representing its data in a disconnected fashion.
   * <p>This implementation creates a Framework {@link ResultSetWrappingSqlRowSet}
   * instance that wraps a standard JDBC {@link CachedRowSet} instance.
   * Can be overridden to use a different implementation.
   *
   * @param rs the original ResultSet (connected)
   * @return the disconnected SqlRowSet
   * @throws SQLException if thrown by JDBC methods
   * @see #newCachedRowSet()
   * @see cn.taketoday.jdbc.support.rowset.ResultSetWrappingSqlRowSet
   */
  protected SqlRowSet createSqlRowSet(ResultSet rs) throws SQLException {
    CachedRowSet rowSet = newCachedRowSet();
    rowSet.populate(rs);
    return new ResultSetWrappingSqlRowSet(rowSet);
  }

  /**
   * Create a new {@link CachedRowSet} instance, to be populated by
   * the {@code createSqlRowSet} implementation.
   * <p>The default implementation uses JDBC 4.1's {@link RowSetFactory}.
   *
   * @return a new CachedRowSet instance
   * @throws SQLException if thrown by JDBC methods
   * @see #createSqlRowSet
   * @see RowSetProvider#newFactory()
   * @see RowSetFactory#createCachedRowSet()
   */
  protected CachedRowSet newCachedRowSet() throws SQLException {
    return rowSetFactory.createCachedRowSet();
  }

}
