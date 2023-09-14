/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Nullable;

/**
 * Interface that defines common functionality for objects that can
 * offer parameter values for named SQL parameters, serving as argument
 * for {@link NamedParameterJdbcTemplate} operations.
 *
 * <p>This interface allows for the specification of SQL type in addition
 * to parameter values. All parameter values and types are identified by
 * specifying the name of the parameter.
 *
 * <p>Intended to wrap various implementations like a Map or a JavaBean
 * with a consistent interface.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NamedParameterJdbcOperations
 * @see NamedParameterJdbcTemplate
 * @see MapSqlParameterSource
 * @see BeanPropertySqlParameterSource
 * @since 4.0
 */
public interface SqlParameterSource {

  /**
   * Constant that indicates an unknown (or unspecified) SQL type.
   * To be returned from {@code getType} when no specific SQL type known.
   *
   * @see #getSqlType
   * @see java.sql.Types
   */
  int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;

  /**
   * Determine whether there is a value for the specified named parameter.
   *
   * @param paramName the name of the parameter
   * @return whether there is a value defined
   */
  boolean hasValue(String paramName);

  /**
   * Return the parameter value for the requested named parameter.
   *
   * @param paramName the name of the parameter
   * @return the value of the specified parameter
   * @throws IllegalArgumentException if there is no value for the requested parameter
   */
  @Nullable
  Object getValue(String paramName) throws IllegalArgumentException;

  /**
   * Determine the SQL type for the specified named parameter.
   *
   * @param paramName the name of the parameter
   * @return the SQL type of the specified parameter,
   * or {@code TYPE_UNKNOWN} if not known
   * @see #TYPE_UNKNOWN
   */
  default int getSqlType(String paramName) {
    return TYPE_UNKNOWN;
  }

  /**
   * Determine the type name for the specified named parameter.
   *
   * @param paramName the name of the parameter
   * @return the type name of the specified parameter,
   * or {@code null} if not known
   */
  @Nullable
  default String getTypeName(String paramName) {
    return null;
  }

  /**
   * Enumerate all available parameter names if possible.
   * <p>This is an optional operation, primarily for use with
   * {@link cn.taketoday.jdbc.core.simple.SimpleJdbcInsert}
   * and {@link cn.taketoday.jdbc.core.simple.SimpleJdbcCall}.
   *
   * @return the array of parameter names, or {@code null} if not determinable
   * @see SqlParameterSourceUtils#extractCaseInsensitiveParameterNames
   */
  @Nullable
  default String[] getParameterNames() {
    return null;
  }

}
