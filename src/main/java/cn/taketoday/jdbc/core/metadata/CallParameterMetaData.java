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

package cn.taketoday.jdbc.core.metadata;

import java.sql.DatabaseMetaData;

import cn.taketoday.lang.Nullable;

/**
 * Holder of meta-data for a specific parameter that is used for call processing.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see GenericCallMetaDataProvider
 * @since 4.0
 */
public class CallParameterMetaData {

  private final boolean function;

  @Nullable
  private final String parameterName;

  private final int parameterType;

  private final int sqlType;

  @Nullable
  private final String typeName;

  private final boolean nullable;

  /**
   * Constructor taking all the properties including the function marker.
   *
   * @since 4.0
   */
  public CallParameterMetaData(boolean function, @Nullable String columnName, int columnType,
                               int sqlType, @Nullable String typeName, boolean nullable) {

    this.function = function;
    this.parameterName = columnName;
    this.parameterType = columnType;
    this.sqlType = sqlType;
    this.typeName = typeName;
    this.nullable = nullable;
  }

  /**
   * Return whether this parameter is declared in a function.
   *
   * @since 4.0
   */
  public boolean isFunction() {
    return this.function;
  }

  /**
   * Return the parameter name.
   */
  @Nullable
  public String getParameterName() {
    return this.parameterName;
  }

  /**
   * Return the parameter type.
   */
  public int getParameterType() {
    return this.parameterType;
  }

  /**
   * Determine whether the declared parameter qualifies as a 'return' parameter
   * for our purposes: type {@link DatabaseMetaData#procedureColumnReturn} or
   * {@link DatabaseMetaData#procedureColumnResult}, or in case of a function,
   * {@link DatabaseMetaData#functionReturn}.
   *
   * @since 4.0
   */
  public boolean isReturnParameter() {
    return (this.function ? this.parameterType == DatabaseMetaData.functionReturn :
            (this.parameterType == DatabaseMetaData.procedureColumnReturn ||
                    this.parameterType == DatabaseMetaData.procedureColumnResult));
  }

  /**
   * Return the parameter SQL type.
   */
  public int getSqlType() {
    return this.sqlType;
  }

  /**
   * Return the parameter type name.
   */
  @Nullable
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * Return whether the parameter is nullable.
   */
  public boolean isNullable() {
    return this.nullable;
  }

}
