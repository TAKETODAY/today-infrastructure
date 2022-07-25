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

/**
 * @author Clinton Begin
 */
public class SqlTimestampTypeHandler extends BaseTypeHandler<Timestamp> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Timestamp parameter)
          throws SQLException {
    ps.setTimestamp(i, parameter);
  }

  @Override
  public Timestamp getResult(ResultSet rs, String columnName)
          throws SQLException {
    return rs.getTimestamp(columnName);
  }

  @Override
  public Timestamp getResult(ResultSet rs, int columnIndex)
          throws SQLException {
    return rs.getTimestamp(columnIndex);
  }

  @Override
  public Timestamp getResult(CallableStatement cs, int columnIndex)
          throws SQLException {
    return cs.getTimestamp(columnIndex);
  }
}
