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

/**
 * @author TODAY
 */
public class LongTypeHandler extends BaseTypeHandler<Long> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Long parameter) throws SQLException {
    ps.setLong(i, parameter);
  }

  @Override
  public Long getResult(ResultSet rs, String columnName) throws SQLException {
    long result = rs.getLong(columnName);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
    long result = rs.getLong(columnIndex);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Long getResult(CallableStatement cs, int columnIndex) throws SQLException {
    long result = cs.getLong(columnIndex);
    return result == 0 && cs.wasNull() ? null : result;
  }
}
