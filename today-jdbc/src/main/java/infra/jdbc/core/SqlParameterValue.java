/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.core;

import infra.lang.Nullable;

/**
 * Object to represent an SQL parameter value, including parameter meta-data
 * such as the SQL type and the scale for numeric values.
 *
 * <p>Designed for use with {@link JdbcTemplate}'s operations that take an array of
 * argument values: Each such argument value may be a {@code SqlParameterValue},
 * indicating the SQL type (and optionally the scale) instead of letting the
 * template guess a default type. Note that this only applies to the operations with
 * a 'plain' argument array, not to the overloaded variants with an explicit type array.
 *
 * @author Juergen Hoeller
 * @see java.sql.Types
 * @see JdbcTemplate#query(String, ResultSetExtractor, Object[])
 * @see JdbcTemplate#query(String, RowCallbackHandler, Object[])
 * @see JdbcTemplate#query(String, RowMapper, Object[])
 * @see JdbcTemplate#update(String, Object[])
 * @since 4.0
 */
public class SqlParameterValue extends SqlParameter {

  @Nullable
  private final Object value;

  /**
   * Create a new SqlParameterValue, supplying the SQL type.
   *
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param value the value object
   */
  public SqlParameterValue(int sqlType, @Nullable Object value) {
    super(sqlType);
    this.value = value;
  }

  /**
   * Create a new SqlParameterValue, supplying the SQL type.
   *
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   * @param value the value object
   */
  public SqlParameterValue(int sqlType, @Nullable String typeName, @Nullable Object value) {
    super(sqlType, typeName);
    this.value = value;
  }

  /**
   * Create a new SqlParameterValue, supplying the SQL type.
   *
   * @param sqlType the SQL type of the parameter according to {@code java.sql.Types}
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   * @param value the value object
   */
  public SqlParameterValue(int sqlType, int scale, @Nullable Object value) {
    super(sqlType, scale);
    this.value = value;
  }

  /**
   * Create a new SqlParameterValue based on the given SqlParameter declaration.
   *
   * @param declaredParam the declared SqlParameter to define a value for
   * @param value the value object
   */
  public SqlParameterValue(SqlParameter declaredParam, @Nullable Object value) {
    super(declaredParam);
    this.value = value;
  }

  /**
   * Return the value object that this parameter value holds.
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

}
