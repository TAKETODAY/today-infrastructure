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

import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The {@link TypeHandler} for {@link Clob}/{@link Reader} using method supported at JDBC 4.0.
 *
 * @author Kazuki Shimizu
 * @since 4.0
 */
public class ClobReaderTypeHandler extends BaseTypeHandler<Reader> {

  /**
   * Set a {@link Reader} into {@link PreparedStatement}.
   *
   * @see PreparedStatement#setClob(int, Reader)
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Reader parameter)
          throws SQLException {
    ps.setClob(i, parameter);
  }

  /**
   * Get a {@link Reader} that corresponds to a specified column name from {@link ResultSet}.
   *
   * @see ResultSet#getClob(String)
   */
  @Override
  public Reader getResult(ResultSet rs, String columnName)
          throws SQLException {
    return toReader(rs.getClob(columnName));
  }

  /**
   * Get a {@link Reader} that corresponds to a specified column index from {@link ResultSet}.
   *
   * @see ResultSet#getClob(int)
   */
  @Override
  public Reader getResult(ResultSet rs, int columnIndex)
          throws SQLException {
    return toReader(rs.getClob(columnIndex));
  }

  /**
   * Get a {@link Reader} that corresponds to a specified column index from {@link CallableStatement}.
   *
   * @see CallableStatement#getClob(int)
   */
  @Override
  public Reader getResult(CallableStatement cs, int columnIndex)
          throws SQLException {
    return toReader(cs.getClob(columnIndex));
  }

  private Reader toReader(Clob clob) throws SQLException {
    if (clob == null) {
      return null;
    }
    else {
      return clob.getCharacterStream();
    }
  }

}
