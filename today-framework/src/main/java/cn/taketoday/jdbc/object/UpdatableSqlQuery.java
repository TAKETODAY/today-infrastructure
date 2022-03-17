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
 * the abstract updateRow(ResultSet, int, context) method to update each
 * row of the JDBC ResultSet and optionally map contents into an object.
 *
 * <p>Subclasses can be constructed providing SQL, parameter types
 * and a DataSource. SQL will often vary between subclasses.
 *
 * @param <T> the result type
 * @author Thomas Risberg
 * @see cn.taketoday.jdbc.object.SqlQuery
 */
public abstract class UpdatableSqlQuery<T> extends SqlQuery<T> {

  /**
   * Constructor to allow use as a JavaBean.
   */
  public UpdatableSqlQuery() {
    setUpdatableResults(true);
  }

  /**
   * Convenient constructor with DataSource and SQL string.
   *
   * @param ds the DataSource to use to get connections
   * @param sql the SQL to run
   */
  public UpdatableSqlQuery(DataSource ds, String sql) {
    super(ds, sql);
    setUpdatableResults(true);
  }

  /**
   * Implementation of the superclass template method. This invokes the subclass's
   * implementation of the {@code updateRow()} method.
   */
  @Override
  protected RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
    return new RowMapperImpl(context);
  }

  /**
   * Subclasses must implement this method to update each row of the
   * ResultSet and optionally create object of the result type.
   *
   * @param rs the ResultSet we're working through
   * @param rowNum row number (from 0) we're up to
   * @param context passed to the execute() method.
   * It can be {@code null} if no contextual information is need.  If you
   * need to pass in data for each row, you can pass in a HashMap with
   * the primary key of the row being the key for the HashMap.  That way
   * it is easy to locate the updates for each row
   * @return an object of the result type
   * @throws SQLException if there's an error updateing data.
   * Subclasses can simply not catch SQLExceptions, relying on the
   * framework to clean up.
   */
  protected abstract T updateRow(ResultSet rs, int rowNum, @Nullable Map<?, ?> context) throws SQLException;

  /**
   * Implementation of RowMapper that calls the enclosing
   * class's {@code updateRow()} method for each row.
   */
  protected class RowMapperImpl implements RowMapper<T> {

    @Nullable
    private final Map<?, ?> context;

    public RowMapperImpl(@Nullable Map<?, ?> context) {
      this.context = context;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
      T result = updateRow(rs, rowNum, this.context);
      rs.updateRow();
      return result;
    }
  }

}
