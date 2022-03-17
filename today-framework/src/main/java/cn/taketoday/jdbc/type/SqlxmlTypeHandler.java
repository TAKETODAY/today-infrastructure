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
import java.sql.SQLXML;

/**
 * Convert <code>String</code> to/from <code>SQLXML</code>.
 *
 * @author Iwao AVE!
 * @since 4.0
 */
public class SqlxmlTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter)
          throws SQLException {
    SQLXML sqlxml = ps.getConnection().createSQLXML();
    try {
      sqlxml.setString(parameter);
      ps.setSQLXML(i, sqlxml);
    }
    finally {
      sqlxml.free();
    }
  }

  @Override
  public String getResult(ResultSet rs, String columnName) throws SQLException {
    return sqlxmlToString(rs.getSQLXML(columnName));
  }

  @Override
  public String getResult(ResultSet rs, int columnIndex) throws SQLException {
    return sqlxmlToString(rs.getSQLXML(columnIndex));
  }

  @Override
  public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return sqlxmlToString(cs.getSQLXML(columnIndex));
  }

  protected String sqlxmlToString(SQLXML sqlxml) throws SQLException {
    if (sqlxml == null) {
      return null;
    }
    try {
      return sqlxml.getString();
    }
    finally {
      sqlxml.free();
    }
  }

}
