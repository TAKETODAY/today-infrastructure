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

package infra.jdbc.type;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ObjectTypeHandler extends BaseTypeHandler<Object> {
  public static final ObjectTypeHandler sharedInstance = new ObjectTypeHandler();

  @Override
  public Object getResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName);
  }

  @Override
  public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getObject(columnIndex);
  }

  @Override
  public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex);
  }

}