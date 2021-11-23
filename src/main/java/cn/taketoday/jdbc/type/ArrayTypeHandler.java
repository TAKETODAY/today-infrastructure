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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @author Clinton Begin
 */
public class ArrayTypeHandler extends BaseTypeHandler<Object> {
  public static final HashMap<Class<?>, String> STANDARD_MAPPING;

  static {
    STANDARD_MAPPING = new HashMap<>();
    STANDARD_MAPPING.put(BigDecimal.class, "NUMERIC");
    STANDARD_MAPPING.put(BigInteger.class, "BIGINT");
    STANDARD_MAPPING.put(boolean.class, "BOOLEAN");
    STANDARD_MAPPING.put(Boolean.class, "BOOLEAN");
    STANDARD_MAPPING.put(byte[].class, "VARBINARY");
    STANDARD_MAPPING.put(byte.class, "TINYINT");
    STANDARD_MAPPING.put(Byte.class, "TINYINT");
    STANDARD_MAPPING.put(Calendar.class, "TIMESTAMP");
    STANDARD_MAPPING.put(java.sql.Date.class, "DATE");
    STANDARD_MAPPING.put(java.util.Date.class, "TIMESTAMP");
    STANDARD_MAPPING.put(double.class, "DOUBLE");
    STANDARD_MAPPING.put(Double.class, "DOUBLE");
    STANDARD_MAPPING.put(float.class, "REAL");
    STANDARD_MAPPING.put(Float.class, "REAL");
    STANDARD_MAPPING.put(int.class, "INTEGER");
    STANDARD_MAPPING.put(Integer.class, "INTEGER");
    STANDARD_MAPPING.put(LocalDate.class, "DATE");
    STANDARD_MAPPING.put(LocalDateTime.class, "TIMESTAMP");
    STANDARD_MAPPING.put(LocalTime.class, "TIME");
    STANDARD_MAPPING.put(long.class, "BIGINT");
    STANDARD_MAPPING.put(Long.class, "BIGINT");
    STANDARD_MAPPING.put(OffsetDateTime.class, "TIMESTAMP_WITH_TIMEZONE");
    STANDARD_MAPPING.put(OffsetTime.class, "TIME_WITH_TIMEZONE");
    STANDARD_MAPPING.put(Short.class, "SMALLINT");
    STANDARD_MAPPING.put(String.class, "VARCHAR");
    STANDARD_MAPPING.put(Time.class, "TIME");
    STANDARD_MAPPING.put(Timestamp.class, "TIMESTAMP");
    STANDARD_MAPPING.put(URL.class, "DATALINK");
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Object parameter) throws SQLException {
    if (parameter instanceof Array) {
      // it's the user's responsibility to properly free() the Array instance
      ps.setArray(parameterIndex, (Array) parameter);
    }
    else {
      if (!parameter.getClass().isArray()) {
        throw new TypeException(
                "ArrayType Handler requires SQL array or java array parameter and does not support type "
                        + parameter.getClass());
      }
      Class<?> componentType = parameter.getClass().getComponentType();
      String arrayTypeName = resolveTypeName(componentType);
      Array array = ps.getConnection().createArrayOf(arrayTypeName, (Object[]) parameter);
      ps.setArray(parameterIndex, array);
      array.free();
    }
  }

  protected String resolveTypeName(Class<?> type) {
    return STANDARD_MAPPING.getOrDefault(type, "JAVA_OBJECT");
  }

  @Override
  public Object getResult(ResultSet rs, String columnName) throws SQLException {
    return extractArray(rs.getArray(columnName));
  }

  @Override
  public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    return extractArray(rs.getArray(columnIndex));
  }

  @Override
  public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return extractArray(cs.getArray(columnIndex));
  }

  protected Object extractArray(Array array) throws SQLException {
    if (array == null) {
      return null;
    }
    Object result = array.getArray();
    array.free();
    return result;
  }

}
