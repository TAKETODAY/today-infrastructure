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

package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/6/20 22:22
 */
public final class AnyTypeHandler<T> implements TypeHandler<T> {

  private final Class<T> type;

  public AnyTypeHandler(Class<T> type) {
    Assert.notNull(type, "type is required");
    this.type = type;
  }

  @Override
  public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable T parameter) throws SQLException {
    ps.setObject(parameterIndex, parameter);
  }

  @Nullable
  @Override
  public T getResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName, type);
  }

  @Nullable
  @Override
  public T getResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getObject(columnIndex, type);
  }

  @Nullable
  @Override
  public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex, type);
  }

}
