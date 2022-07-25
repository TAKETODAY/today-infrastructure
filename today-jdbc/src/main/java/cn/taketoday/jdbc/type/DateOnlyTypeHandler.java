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
import java.util.Date;

/**
 * @author Clinton Begin
 */
public class DateOnlyTypeHandler extends BaseTypeHandler<Date> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Date parameter) throws SQLException {
    ps.setDate(i, new java.sql.Date(parameter.getTime()));
  }

  @Override
  public Date getResult(ResultSet rs, String columnName)
          throws SQLException {
    java.sql.Date sqlDate = rs.getDate(columnName);
    if (sqlDate != null) {
      return new Date(sqlDate.getTime());
    }
    return null;
  }

  @Override
  public Date getResult(ResultSet rs, int columnIndex)
          throws SQLException {
    java.sql.Date sqlDate = rs.getDate(columnIndex);
    if (sqlDate != null) {
      return new Date(sqlDate.getTime());
    }
    return null;
  }

  @Override
  public Date getResult(CallableStatement cs, int columnIndex)
          throws SQLException {
    java.sql.Date sqlDate = cs.getDate(columnIndex);
    if (sqlDate != null) {
      return new Date(sqlDate.getTime());
    }
    return null;
  }

}
