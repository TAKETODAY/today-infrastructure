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

package cn.taketoday.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.support.JdbcUtils;

/**
 * Abstract base class for {@link DataFieldMaxValueIncrementer} implementations that use
 * a database sequence. Subclasses need to provide the database-specific SQL to use.
 *
 * @author Juergen Hoeller
 * @see #getSequenceQuery
 * @since 4.0
 */
public abstract class AbstractSequenceMaxValueIncrementer extends AbstractDataFieldMaxValueIncrementer {

  /**
   * Default constructor for bean property style usage.
   *
   * @see #setDataSource
   * @see #setIncrementerName
   */
  public AbstractSequenceMaxValueIncrementer() { }

  /**
   * Convenience constructor.
   *
   * @param dataSource the DataSource to use
   * @param incrementerName the name of the sequence/table to use
   */
  public AbstractSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
    super(dataSource, incrementerName);
  }

  /**
   * Executes the SQL as specified by {@link #getSequenceQuery()}.
   */
  @Override
  protected long getNextKey() throws DataAccessException {
    Connection con = DataSourceUtils.getConnection(getDataSource());
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = con.createStatement();
      DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
      rs = stmt.executeQuery(getSequenceQuery());
      if (rs.next()) {
        return rs.getLong(1);
      }
      else {
        throw new DataAccessResourceFailureException("Sequence query did not return a result");
      }
    }
    catch (SQLException ex) {
      throw new DataAccessResourceFailureException("Could not obtain sequence value", ex);
    }
    finally {
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(stmt);
      DataSourceUtils.releaseConnection(con, getDataSource());
    }
  }

  /**
   * Return the database-specific query to use for retrieving a sequence value.
   * <p>The provided SQL is supposed to result in a single row with a single
   * column that allows for extracting a {@code long} value.
   */
  protected abstract String getSequenceQuery();

}
