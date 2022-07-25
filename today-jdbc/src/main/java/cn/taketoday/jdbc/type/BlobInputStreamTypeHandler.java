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

import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The {@link TypeHandler} for {@link Blob}/{@link InputStream} using method supported at JDBC 4.0.
 *
 * @author Kazuki Shimizu
 */
public class BlobInputStreamTypeHandler extends BaseTypeHandler<InputStream> {

  /**
   * Set an {@link InputStream} into {@link PreparedStatement}.
   *
   * @see PreparedStatement#setBlob(int, InputStream)
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, InputStream parameter) throws SQLException {
    ps.setBlob(i, parameter);
  }

  /**
   * Get an {@link InputStream} that corresponds to a specified column name from {@link ResultSet}.
   *
   * @see ResultSet#getBlob(String)
   */
  @Override
  public InputStream getResult(ResultSet rs, String columnName) throws SQLException {
    return toInputStream(rs.getBlob(columnName));
  }

  /**
   * Get an {@link InputStream} that corresponds to a specified column index from {@link ResultSet}.
   *
   * @see ResultSet#getBlob(int)
   */
  @Override
  public InputStream getResult(ResultSet rs, int columnIndex) throws SQLException {
    return toInputStream(rs.getBlob(columnIndex));
  }

  /**
   * Get an {@link InputStream} that corresponds to a specified column index from {@link CallableStatement}.
   *
   * @see CallableStatement#getBlob(int)
   */
  @Override
  public InputStream getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toInputStream(cs.getBlob(columnIndex));
  }

  static InputStream toInputStream(Blob blob) throws SQLException {
    if (blob == null) {
      return null;
    }
    return blob.getBinaryStream();
  }

}
