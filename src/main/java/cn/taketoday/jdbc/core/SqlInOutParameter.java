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

/**
 * Subclass of {@link SqlOutParameter} to represent an INOUT parameter.
 * Will return {@code true} for SqlParameter's {@link #isInputValueProvided}
 * test, in contrast to a standard SqlOutParameter.
 *
 * <p>Output parameters - like all stored procedure parameters - must have names.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SqlInOutParameter extends SqlOutParameter {

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   */
  public SqlInOutParameter(String name, int sqlType) {
    super(name, sqlType);
  }

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   */
  public SqlInOutParameter(String name, int sqlType, int scale) {
    super(name, sqlType, scale);
  }

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   */
  public SqlInOutParameter(String name, int sqlType, String typeName) {
    super(name, sqlType, typeName);
  }

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   * @param sqlReturnType custom value handler for complex type (optional)
   */
  public SqlInOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
    super(name, sqlType, typeName, sqlReturnType);
  }

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
   */
  public SqlInOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
    super(name, sqlType, rse);
  }

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
   */
  public SqlInOutParameter(String name, int sqlType, RowCallbackHandler rch) {
    super(name, sqlType, rch);
  }

  /**
   * Create a new SqlInOutParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
   */
  public SqlInOutParameter(String name, int sqlType, RowMapper<?> rm) {
    super(name, sqlType, rm);
  }

  /**
   * This implementation always returns {@code true}.
   */
  @Override
  public boolean isInputValueProvided() {
    return true;
  }

}
