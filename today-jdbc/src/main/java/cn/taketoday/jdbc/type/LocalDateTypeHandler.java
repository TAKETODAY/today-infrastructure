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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * @author Tomas Rohovsky
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

  @Override
  public LocalDate getResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName, LocalDate.class);
  }

  @Override
  public LocalDate getResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getObject(columnIndex, LocalDate.class);
  }

  @Override
  public LocalDate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex, LocalDate.class);
  }

}
