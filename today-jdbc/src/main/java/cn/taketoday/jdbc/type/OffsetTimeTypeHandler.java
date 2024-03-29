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
import java.time.OffsetTime;

/**
 * @author Tomas Rohovsky
 */
public class OffsetTimeTypeHandler extends BaseTypeHandler<OffsetTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, OffsetTime parameter) throws SQLException {
    ps.setTime(i, Time.valueOf(parameter.toLocalTime()));
  }

  @Override
  public OffsetTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getOffsetTime(rs.getTime(columnName));
  }

  @Override
  public OffsetTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getOffsetTime(rs.getTime(columnIndex));
  }

  @Override
  public OffsetTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getOffsetTime(cs.getTime(columnIndex));
  }

  static OffsetTime getOffsetTime(Time time) {
    if (time != null) {
      return time.toLocalTime().atOffset(OffsetTime.now().getOffset());
    }
    return null;
  }
}
