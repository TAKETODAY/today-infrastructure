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

import org.joda.time.LocalDate;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/1/6 16:04
 */
@MappedTypes(LocalDate.class)
public class JodaLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter) throws SQLException {
    ps.setDate(i, new Date(parameter.toDateTimeAtStartOfDay().getMillis()));
  }

  @Override
  public LocalDate getResult(ResultSet rs, String columnName) throws SQLException {
    return getResultInternal(rs.getObject(columnName));
  }

  @Override
  public LocalDate getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getResultInternal(rs.getObject(columnIndex));
  }

  @Override
  public LocalDate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getResultInternal(cs.getObject(columnIndex));
  }

  @Nullable
  protected LocalDate getResultInternal(@Nullable Object val) {
    if (val == null) {
      return null;
    }
    try {
      // Joda has it's own pluggable converters infrastructure
      // it will throw IllegalArgumentException if can't convert
      // look @ org.joda.time.convert.ConverterManager
      return new LocalDate(val);
    }
    catch (IllegalArgumentException ex) {
      throw new TypeException(
              "Don't know how to convert from type '" + val.getClass().getName() + "' to type '"
                      + LocalDate.class.getName() + "'", ex);
    }
  }
}
