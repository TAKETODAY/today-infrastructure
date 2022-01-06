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

import cn.taketoday.jdbc.core.RowMapper;
import cn.taketoday.lang.Nullable;

/**
 * Reusable RDBMS query in which concrete subclasses must implement
 * the abstract mapRow(ResultSet, int) method to map each row of
 * the JDBC ResultSet into an object.
 *
 * <p>Such manual mapping is usually preferable to "automatic"
 * mapping using reflection, which can become complex in non-trivial
 * cases. For example, the present class allows different objects
 * to be used for different rows (for example, if a subclass is indicated).
 * It allows computed fields to be set. And there's no need for
 * ResultSet columns to have the same names as bean properties.
 * The Pareto Principle in action: going the extra mile to automate
 * the extraction process makes the framework much more complex
 * and delivers little real benefit.
 *
 * <p>Subclasses can be constructed providing SQL, parameter types
 * and a DataSource. SQL will often vary between subclasses.
 *
 * @param <T> the result type
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Jean-Pierre Pawlak
 * @see cn.taketoday.jdbc.object.MappingSqlQuery
 * @see cn.taketoday.jdbc.object.SqlQuery
 */
public abstract class MappingSqlQueryWithParameters<T> extends SqlQuery<T> {

  /**
   * Constructor to allow use as a JavaBean.
   */
  public MappingSqlQueryWithParameters() {
  }

  /**
   * Convenient constructor with DataSource and SQL string.
   *
   * @param ds the DataSource to use to get connections
   * @param sql the SQL to run
   */
  public MappingSqlQueryWithParameters(DataSource ds, String sql) {
    super(ds, sql);
  }

  /**
   * Implementation of protected abstract method. This invokes the subclass's
   * implementation of the mapRow() method.
   */
  @Override
  protected RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
    return new RowMapperImpl(parameters, context);
  }

  /**
   * Subclasses must implement this method to convert each row
   * of the ResultSet into an object of the result type.
   *
   * @param rs the ResultSet we're working through
   * @param rowNum row number (from 0) we're up to
   * @param parameters to the query (passed to the execute() method).
   * Subclasses are rarely interested in these.
   * It can be {@code null} if there are no parameters.
   * @param context passed to the execute() method.
   * It can be {@code null} if no contextual information is need.
   * @return an object of the result type
   * @throws SQLException if there's an error extracting data.
   * Subclasses can simply not catch SQLExceptions, relying on the
   * framework to clean up.
   */
  @Nullable
  protected abstract T mapRow(ResultSet rs, int rowNum, @Nullable Object[] parameters, @Nullable Map<?, ?> context)
          throws SQLException;

  /**
   * Implementation of RowMapper that calls the enclosing
   * class's {@code mapRow} method for each row.
   */
  protected class RowMapperImpl implements RowMapper<T> {

    @Nullable
    private final Object[] params;

    @Nullable
    private final Map<?, ?> context;

    /**
     * Use an array results. More efficient if we know how many results to expect.
     */
    public RowMapperImpl(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
      this.params = parameters;
      this.context = context;
    }

    @Override
    @Nullable
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
      return MappingSqlQueryWithParameters.this.mapRow(rs, rowNum, this.params, this.context);
    }
  }

}
