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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Object to represent an SQL parameter definition.
 *
 * <p>Parameters may be anonymous, in which case "name" is {@code null}.
 * However, all parameters must define an SQL type according to {@link java.sql.Types}.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see java.sql.Types
 */
public class SqlParameter {

  // The name of the parameter, if any
  @Nullable
  private String name;

  // SQL type constant from {@code java.sql.Types}
  private final int sqlType;

  // Used for types that are user-named like: STRUCT, DISTINCT, JAVA_OBJECT, named array types
  @Nullable
  private String typeName;

  // The scale to apply in case of a NUMERIC or DECIMAL type, if any
  @Nullable
  private Integer scale;

  /**
   * Create a new anonymous SqlParameter, supplying the SQL type.
   *
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   */
  public SqlParameter(int sqlType) {
    this.sqlType = sqlType;
  }

  /**
   * Create a new anonymous SqlParameter, supplying the SQL type.
   *
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   */
  public SqlParameter(int sqlType, @Nullable String typeName) {
    this.sqlType = sqlType;
    this.typeName = typeName;
  }

  /**
   * Create a new anonymous SqlParameter, supplying the SQL type.
   *
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   */
  public SqlParameter(int sqlType, int scale) {
    this.sqlType = sqlType;
    this.scale = scale;
  }

  /**
   * Create a new SqlParameter, supplying name and SQL type.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   */
  public SqlParameter(String name, int sqlType) {
    this.name = name;
    this.sqlType = sqlType;
  }

  /**
   * Create a new SqlParameter, supplying name and SQL type.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   */
  public SqlParameter(String name, int sqlType, @Nullable String typeName) {
    this.name = name;
    this.sqlType = sqlType;
    this.typeName = typeName;
  }

  /**
   * Create a new SqlParameter, supplying name and SQL type.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   */
  public SqlParameter(String name, int sqlType, int scale) {
    this.name = name;
    this.sqlType = sqlType;
    this.scale = scale;
  }

  /**
   * Copy constructor.
   *
   * @param otherParam the SqlParameter object to copy from
   */
  public SqlParameter(SqlParameter otherParam) {
    Assert.notNull(otherParam, "SqlParameter object must not be null");
    this.name = otherParam.name;
    this.sqlType = otherParam.sqlType;
    this.typeName = otherParam.typeName;
    this.scale = otherParam.scale;
  }

  /**
   * Return the name of the parameter, or {@code null} if anonymous.
   */
  @Nullable
  public String getName() {
    return this.name;
  }

  /**
   * Return the SQL type of the parameter.
   */
  public int getSqlType() {
    return this.sqlType;
  }

  /**
   * Return the type name of the parameter, if any.
   */
  @Nullable
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * Return the scale of the parameter, if any.
   */
  @Nullable
  public Integer getScale() {
    return this.scale;
  }

  /**
   * Return whether this parameter holds input values that should be set
   * before execution even if they are {@code null}.
   * <p>This implementation always returns {@code true}.
   */
  public boolean isInputValueProvided() {
    return true;
  }

  /**
   * Return whether this parameter is an implicit return parameter used during the
   * results processing of {@code CallableStatement.getMoreResults/getUpdateCount}.
   * <p>This implementation always returns {@code false}.
   */
  public boolean isResultsParameter() {
    return false;
  }

  /**
   * Convert a list of JDBC types, as defined in {@code java.sql.Types},
   * to a List of SqlParameter objects as used in this package.
   */
  public static List<SqlParameter> sqlTypesToAnonymousParameterList(@Nullable int... types) {
    if (types == null) {
      return new ArrayList<>();
    }
    ArrayList<SqlParameter> result = new ArrayList<>(types.length);
    for (int type : types) {
      result.add(new SqlParameter(type));
    }
    return result;
  }

}
