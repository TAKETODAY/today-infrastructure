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
package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.PersistenceException;

/**
 * Iterator for a {@link ResultSet}. Tricky part here is getting
 * {@link #hasNext()} to work properly, meaning it can be called multiple times
 * without calling {@link #next()}.
 *
 * @author aldenquimby@gmail.com
 * @author TODAY
 */
public final class ResultSetHandlerIterator<T> extends ResultSetIterator<T> {
  private final ResultSetHandler<T> handler;

  public ResultSetHandlerIterator(ResultSet rs, ResultSetHandler<T> handler) {
    super(rs);
    this.handler = handler;
  }

  public ResultSetHandlerIterator(ResultSet rs, ResultSetHandlerFactory<T> factory) {
    super(rs);
    try {
      this.handler = factory.getResultSetHandler(rs.getMetaData());
    }
    catch (SQLException e) {
      throw new PersistenceException("Database error: " + e.getMessage(), e);
    }
  }

  @Override
  protected T readNext() throws SQLException {
    return handler.handle(resultSet);
  }

}
