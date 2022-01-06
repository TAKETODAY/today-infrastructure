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

package cn.taketoday.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.lang.Nullable;

/**
 * Reusable query in which concrete subclasses must implement the abstract
 * mapRow(ResultSet, int) method to convert each row of the JDBC ResultSet
 * into an object.
 *
 * <p>Simplifies MappingSqlQueryWithParameters API by dropping parameters and
 * context. Most subclasses won't care about parameters. If you don't use
 * contextual information, subclass this instead of MappingSqlQueryWithParameters.
 *
 * @param <T> the result type
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Jean-Pierre Pawlak
 * @see MappingSqlQueryWithParameters
 */
public abstract class MappingSqlQuery<T> extends MappingSqlQueryWithParameters<T> {

  /**
   * Constructor that allows use as a JavaBean.
   */
  public MappingSqlQuery() {
  }

  /**
   * Convenient constructor with DataSource and SQL string.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL to run
   */
  public MappingSqlQuery(DataSource ds, String sql) {
    super(ds, sql);
  }

  /**
   * This method is implemented to invoke the simpler mapRow
   * template method, ignoring parameters.
   *
   * @see #mapRow(ResultSet, int)
   */
  @Override
  @Nullable
  protected final T mapRow(ResultSet rs, int rowNum, @Nullable Object[] parameters, @Nullable Map<?, ?> context)
          throws SQLException {

    return mapRow(rs, rowNum);
  }

  /**
   * Subclasses must implement this method to convert each row of the
   * ResultSet into an object of the result type.
   * <p>Subclasses of this class, as opposed to direct subclasses of
   * MappingSqlQueryWithParameters, don't need to concern themselves
   * with the parameters to the execute method of the query object.
   *
   * @param rs the ResultSet we're working through
   * @param rowNum row number (from 0) we're up to
   * @return an object of the result type
   * @throws SQLException if there's an error extracting data.
   * Subclasses can simply not catch SQLExceptions, relying on the
   * framework to clean up.
   */
  @Nullable
  protected abstract T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
