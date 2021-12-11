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

/**
 * Holder of meta-data for a specific parameter that is used for table processing.
 *
 * @author Thomas Risberg
 * @see GenericTableMetaDataProvider
 * @since 4.0
 */
public class TableParameterMetaData {

  private final String parameterName;

  private final int sqlType;

  private final boolean nullable;

  /**
   * Constructor taking all the properties.
   */
  public TableParameterMetaData(String columnName, int sqlType, boolean nullable) {
    this.parameterName = columnName;
    this.sqlType = sqlType;
    this.nullable = nullable;
  }

  /**
   * Get the parameter name.
   */
  public String getParameterName() {
    return this.parameterName;
  }

  /**
   * Get the parameter SQL type.
   */
  public int getSqlType() {
    return this.sqlType;
  }

  /**
   * Get whether the parameter/column is nullable.
   */
  public boolean isNullable() {
    return this.nullable;
  }

}
