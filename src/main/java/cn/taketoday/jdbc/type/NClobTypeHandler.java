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

import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class NClobTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter)
          throws SQLException {
    StringReader reader = new StringReader(parameter);
    ps.setCharacterStream(i, reader, parameter.length());
  }

  @Override
  public String getResult(ResultSet rs, String columnName)
          throws SQLException {
    Clob clob = rs.getClob(columnName);
    return toString(clob);
  }

  @Override
  public String getResult(ResultSet rs, int columnIndex)
          throws SQLException {
    Clob clob = rs.getClob(columnIndex);
    return toString(clob);
  }

  @Override
  public String getResult(CallableStatement cs, int columnIndex)
          throws SQLException {
    Clob clob = cs.getClob(columnIndex);
    return toString(clob);
  }

  private String toString(Clob clob) throws SQLException {
    return clob == null ? null : clob.getSubString(1, (int) clob.length());
  }

}
