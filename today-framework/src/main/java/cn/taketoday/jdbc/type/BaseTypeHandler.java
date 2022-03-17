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

import cn.taketoday.core.TypeReference;

/**
 * The base {@link TypeHandler} for references a generic type.
 * <p>
 * Important: This class never call the {@link ResultSet#wasNull()} and
 * {@link CallableStatement#wasNull()} method for handling the SQL {@code NULL} value.
 * In other words, {@code null} value handling should be performed on subclass.
 * </p>
 *
 * @author Clinton Begin
 * @author Simone Tripodi
 * @author Kzuki Shimizu
 * @author TODAY
 */
public abstract class BaseTypeHandler<T>
        extends TypeReference<T> implements TypeHandler<T> {

  @Override
  public void setParameter(PreparedStatement ps, int parameterIndex, T parameter) throws SQLException {
    if (parameter == null) {
      setNullParameter(ps, parameterIndex);
    }
    else {
      setNonNullParameter(ps, parameterIndex, parameter);
    }
  }

  public void setNullParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    ps.setObject(parameterIndex, null);
  }

  public void setNonNullParameter(PreparedStatement ps, int parameterIndex, T parameter) throws SQLException {
    ps.setObject(parameterIndex, parameter);
  }

}
