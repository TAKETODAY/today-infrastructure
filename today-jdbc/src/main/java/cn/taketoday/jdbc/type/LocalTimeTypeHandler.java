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
package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;

/**
 * @author Tomas Rohovsky
 * @since 4.0
 */
public class LocalTimeTypeHandler extends BaseTypeHandler<LocalTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalTime parameter) throws SQLException {
    ps.setTime(i, Time.valueOf(parameter));
  }

  @Override
  public LocalTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getLocalTime(rs.getTime(columnName));
  }

  @Override
  public LocalTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getLocalTime(rs.getTime(columnIndex));
  }

  @Override
  public LocalTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getLocalTime(cs.getTime(columnIndex));
  }

  static LocalTime getLocalTime(Time time) {
    if (time != null) {
      return time.toLocalTime();
    }
    return null;
  }
}
