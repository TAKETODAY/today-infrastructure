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
 * @author Clinton Begin
 */
public class ByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Byte[] parameter) throws SQLException {
    ps.setBytes(i, convertToPrimitiveArray(parameter));
  }

  @Override
  public Byte[] getResult(ResultSet rs, String columnName) throws SQLException {
    byte[] bytes = rs.getBytes(columnName);
    return getBytes(bytes);
  }

  @Override
  public Byte[] getResult(ResultSet rs, int columnIndex) throws SQLException {
    byte[] bytes = rs.getBytes(columnIndex);
    return getBytes(bytes);
  }

  @Override
  public Byte[] getResult(CallableStatement cs, int columnIndex) throws SQLException {
    byte[] bytes = cs.getBytes(columnIndex);
    return getBytes(bytes);
  }

  private Byte[] getBytes(byte[] bytes) {
    Byte[] returnValue = null;
    if (bytes != null) {
      returnValue = convertToObjectArray(bytes);
    }
    return returnValue;
  }

  static byte[] convertToPrimitiveArray(Byte[] objects) {
    final byte[] bytes = new byte[objects.length];
    for (int i = 0; i < objects.length; i++) {
      bytes[i] = objects[i];
    }
    return bytes;
  }

  static Byte[] convertToObjectArray(byte[] bytes) {
    final Byte[] objects = new Byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      objects[i] = bytes[i];
    }
    return objects;
  }

}
