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

package cn.taketoday.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.StringJoiner;

import cn.taketoday.jdbc.core.SqlParameterValue;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for {@link SqlParameterSource} implementations.
 * Provides registration of SQL types per parameter and a friendly
 * {@link #toString() toString} representation enumerating all parameters for
 * a {@code SqlParameterSource} implementing {@link #getParameterNames()}.
 * Concrete subclasses must implement {@link #hasValue} and {@link #getValue}.
 *
 * @author Juergen Hoeller
 * @author Jens Schauder
 * @see #hasValue(String)
 * @see #getValue(String)
 * @see #getParameterNames()
 * @since 4.0
 */
public abstract class AbstractSqlParameterSource implements SqlParameterSource {

  private final HashMap<String, Integer> sqlTypes = new HashMap<>();
  private final HashMap<String, String> typeNames = new HashMap<>();

  /**
   * Register an SQL type for the given parameter.
   *
   * @param paramName the name of the parameter
   * @param sqlType the SQL type of the parameter
   */
  public void registerSqlType(String paramName, int sqlType) {
    Assert.notNull(paramName, "Parameter name is required");
    this.sqlTypes.put(paramName, sqlType);
  }

  /**
   * Register an SQL type for the given parameter.
   *
   * @param paramName the name of the parameter
   * @param typeName the type name of the parameter
   */
  public void registerTypeName(String paramName, String typeName) {
    Assert.notNull(paramName, "Parameter name is required");
    this.typeNames.put(paramName, typeName);
  }

  /**
   * Return the SQL type for the given parameter, if registered.
   *
   * @param paramName the name of the parameter
   * @return the SQL type of the parameter,
   * or {@code TYPE_UNKNOWN} if not registered
   */
  @Override
  public int getSqlType(String paramName) {
    Assert.notNull(paramName, "Parameter name is required");
    return this.sqlTypes.getOrDefault(paramName, TYPE_UNKNOWN);
  }

  /**
   * Return the type name for the given parameter, if registered.
   *
   * @param paramName the name of the parameter
   * @return the type name of the parameter,
   * or {@code null} if not registered
   */
  @Override
  @Nullable
  public String getTypeName(String paramName) {
    Assert.notNull(paramName, "Parameter name is required");
    return this.typeNames.get(paramName);
  }

  /**
   * Enumerate the parameter names and values with their corresponding SQL type if available,
   * or just return the simple {@code SqlParameterSource} implementation class name otherwise.
   *
   * @see #getParameterNames()
   */
  @Override
  public String toString() {
    String[] parameterNames = getParameterNames();
    if (parameterNames != null) {
      StringJoiner result = new StringJoiner(", ", getClass().getSimpleName() + " {", "}");
      for (String parameterName : parameterNames) {
        Object value = getValue(parameterName);
        if (value instanceof SqlParameterValue) {
          value = ((SqlParameterValue) value).getValue();
        }
        String typeName = getTypeName(parameterName);
        if (typeName == null) {
          int sqlType = getSqlType(parameterName);
          if (sqlType != TYPE_UNKNOWN) {
            typeName = JdbcUtils.resolveTypeName(sqlType);
            if (typeName == null) {
              typeName = String.valueOf(sqlType);
            }
          }
        }
        StringBuilder entry = new StringBuilder();
        entry.append(parameterName).append('=').append(value);
        if (typeName != null) {
          entry.append(" (type:").append(typeName).append(')');
        }
        result.add(entry);
      }
      return result.toString();
    }
    else {
      return getClass().getSimpleName();
    }
  }

}
