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
import java.time.YearMonth;

/**
 * Type Handler for {@link YearMonth}.
 * <p>
 * YearMonthTypeHandler relies upon
 * {@link YearMonth#parse YearMonth.parse}. Therefore column values
 * are expected as strings. The format must be uuuu-MM. Example: "2016-08"
 *
 * @author Björn Raupach
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class YearMonthTypeHandler extends BaseTypeHandler<YearMonth> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, YearMonth yearMonth) throws SQLException {
    ps.setString(i, yearMonth.toString());
  }

  @Override
  public YearMonth getResult(ResultSet rs, String columnName) throws SQLException {
    String value = rs.getString(columnName);
    return value == null ? null : YearMonth.parse(value);
  }

  @Override
  public YearMonth getResult(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    return value == null ? null : YearMonth.parse(value);
  }

  @Override
  public YearMonth getResult(CallableStatement cs, int columnIndex) throws SQLException {
    String value = cs.getString(columnIndex);
    return value == null ? null : YearMonth.parse(value);
  }

}
