/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.type;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/2/12 13:51
 */
public class BytesInputStreamTypeHandler extends BaseTypeHandler<InputStream> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int parameterIndex, InputStream parameter) throws SQLException {
    ps.setBinaryStream(parameterIndex, parameter);
  }

  @Override
  public InputStream getResult(ResultSet rs, String columnName) throws SQLException {
    byte[] bytes = rs.getBytes(columnName);
    return bytes != null ? new ByteArrayInputStream(bytes) : null;
  }

  @Override
  public InputStream getResult(ResultSet rs, int columnIndex) throws SQLException {
    byte[] bytes = rs.getBytes(columnIndex);
    return bytes != null ? new ByteArrayInputStream(bytes) : null;
  }

  @Override
  public InputStream getResult(CallableStatement cs, int columnIndex) throws SQLException {
    byte[] bytes = cs.getBytes(columnIndex);
    return bytes != null ? new ByteArrayInputStream(bytes) : null;
  }

}
