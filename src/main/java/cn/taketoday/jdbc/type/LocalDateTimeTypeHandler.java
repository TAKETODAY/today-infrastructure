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
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author Tomas Rohovsky
 * @since 4.0
 */
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter) throws SQLException {
    ps.setTimestamp(i, Timestamp.valueOf(parameter));
  }

  @Override
  public LocalDateTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getLocalDateTime(rs.getTimestamp(columnName));
  }

  @Override
  public LocalDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getLocalDateTime(rs.getTimestamp(columnIndex));
  }

  @Override
  public LocalDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getLocalDateTime(cs.getTimestamp(columnIndex));
  }

  static LocalDateTime getLocalDateTime(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.toLocalDateTime();
    }
    return null;
  }
}
