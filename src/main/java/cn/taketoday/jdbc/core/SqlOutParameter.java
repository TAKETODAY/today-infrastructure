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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;

import cn.taketoday.lang.Nullable;

/**
 * Subclass of {@link SqlParameter} to represent an output parameter.
 * No additional properties: instanceof will be used to check for such types.
 *
 * <p>Output parameters - like all stored procedure parameters - must have names.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SqlReturnResultSet
 * @see SqlInOutParameter
 */
public class SqlOutParameter extends ResultSetSupportingSqlParameter {

  @Nullable
  private SqlReturnType sqlReturnType;

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   */
  public SqlOutParameter(String name, int sqlType) {
    super(name, sqlType);
  }

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   */
  public SqlOutParameter(String name, int sqlType, int scale) {
    super(name, sqlType, scale);
  }

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   */
  public SqlOutParameter(String name, int sqlType, @Nullable String typeName) {
    super(name, sqlType, typeName);
  }

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   * @param sqlReturnType custom value handler for complex type (optional)
   */
  public SqlOutParameter(String name, int sqlType, @Nullable String typeName, @Nullable SqlReturnType sqlReturnType) {
    super(name, sqlType, typeName);
    this.sqlReturnType = sqlReturnType;
  }

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
   */
  public SqlOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
    super(name, sqlType, rse);
  }

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
   */
  public SqlOutParameter(String name, int sqlType, RowCallbackHandler rch) {
    super(name, sqlType, rch);
  }

  /**
   * Create a new SqlOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
   */
  public SqlOutParameter(String name, int sqlType, RowMapper<?> rm) {
    super(name, sqlType, rm);
  }

  /**
   * Return the custom return type, if any.
   */
  @Nullable
  public SqlReturnType getSqlReturnType() {
    return this.sqlReturnType;
  }

  /**
   * Return whether this parameter holds a custom return type.
   */
  public boolean isReturnTypeSupported() {
    return (this.sqlReturnType != null);
  }

}
