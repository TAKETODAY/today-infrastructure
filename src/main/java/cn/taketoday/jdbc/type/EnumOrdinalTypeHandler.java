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

import cn.taketoday.lang.Assert;

/**
 * Use {@link Enum#ordinal()}
 *
 * @author Clinton Begin
 * @author TODAY
 */
public class EnumOrdinalTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  private final Class<E> type;
  private final E[] enums;

  public EnumOrdinalTypeHandler(Class<E> type) {
    Assert.notNull(type, "Type argument cannot be null");
    this.type = type;
    this.enums = type.getEnumConstants();
    if (this.enums == null) {
      throw new IllegalArgumentException(type.getSimpleName() + " does not represent an enum type.");
    }
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter) throws SQLException {
    ps.setInt(i, parameter.ordinal());
  }

  @Override
  public E getResult(ResultSet rs, String columnName) throws SQLException {
    int ordinal = rs.getInt(columnName);
    if (ordinal == 0 && rs.wasNull()) {
      return null;
    }
    return toOrdinalEnum(ordinal);
  }

  @Override
  public E getResult(ResultSet rs, int columnIndex) throws SQLException {
    int ordinal = rs.getInt(columnIndex);
    if (ordinal == 0 && rs.wasNull()) {
      return null;
    }
    return toOrdinalEnum(ordinal);
  }

  @Override
  public E getResult(CallableStatement cs, int columnIndex) throws SQLException {
    int ordinal = cs.getInt(columnIndex);
    if (ordinal == 0 && cs.wasNull()) {
      return null;
    }
    return toOrdinalEnum(ordinal);
  }

  E toOrdinalEnum(int ordinal) {
    try {
      return enums[ordinal];
    }
    catch (Exception ex) {
      throw new IllegalArgumentException(
              "Cannot convert " + ordinal + " to " + type.getSimpleName() + " by ordinal value.", ex);
    }
  }
}
