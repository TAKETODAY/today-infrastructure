/*
 * Copyright 2017 - 2025 the original author or authors.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Month;

/**
 * @author Björn Raupach
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MonthTypeHandler extends BasicTypeHandler<Month> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Month arg) throws SQLException {
    ps.setInt(i, arg.getValue());
  }

  @Override
  public Month getResult(ResultSet rs, String columnName) throws SQLException {
    int month = rs.getInt(columnName);
    return month == 0 && rs.wasNull() ? null : Month.of(month);
  }

  @Override
  public Month getResult(ResultSet rs, int columnIndex) throws SQLException {
    int month = rs.getInt(columnIndex);
    return month == 0 && rs.wasNull() ? null : Month.of(month);
  }

  @Override
  public Month getResult(CallableStatement cs, int columnIndex) throws SQLException {
    int month = cs.getInt(columnIndex);
    return month == 0 && cs.wasNull() ? null : Month.of(month);
  }

}
