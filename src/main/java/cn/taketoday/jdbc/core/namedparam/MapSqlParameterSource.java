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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.jdbc.core.SqlParameterValue;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link SqlParameterSource} implementation that holds a given Map of parameters.
 *
 * <p>This class is intended for passing in a simple Map of parameter values
 * to the methods of the {@link NamedParameterJdbcTemplate} class.
 *
 * <p>The {@code addValue} methods on this class will make adding several values
 * easier. The methods return a reference to the {@link MapSqlParameterSource}
 * itself, so you can chain several method calls together within a single statement.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see #addValue(String, Object)
 * @see #addValue(String, Object, int)
 * @see #registerSqlType
 * @see NamedParameterJdbcTemplate
 * @since 4.0
 */
public class MapSqlParameterSource extends AbstractSqlParameterSource {

  private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

  /**
   * Create an empty MapSqlParameterSource,
   * with values to be added via {@code addValue}.
   *
   * @see #addValue(String, Object)
   */
  public MapSqlParameterSource() { }

  /**
   * Create a new MapSqlParameterSource, with one value
   * comprised of the supplied arguments.
   *
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @see #addValue(String, Object)
   */
  public MapSqlParameterSource(String paramName, @Nullable Object value) {
    addValue(paramName, value);
  }

  /**
   * Create a new MapSqlParameterSource based on a Map.
   *
   * @param values a Map holding existing parameter values (can be {@code null})
   */
  public MapSqlParameterSource(@Nullable Map<String, ?> values) {
    addValues(values);
  }

  /**
   * Add a parameter to this parameter source.
   *
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public MapSqlParameterSource addValue(String paramName, @Nullable Object value) {
    Assert.notNull(paramName, "Parameter name must not be null");
    this.values.put(paramName, value);
    if (value instanceof SqlParameterValue) {
      registerSqlType(paramName, ((SqlParameterValue) value).getSqlType());
    }
    return this;
  }

  /**
   * Add a parameter to this parameter source.
   *
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @param sqlType the SQL type of the parameter
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public MapSqlParameterSource addValue(String paramName, @Nullable Object value, int sqlType) {
    Assert.notNull(paramName, "Parameter name must not be null");
    this.values.put(paramName, value);
    registerSqlType(paramName, sqlType);
    return this;
  }

  /**
   * Add a parameter to this parameter source.
   *
   * @param paramName the name of the parameter
   * @param value the value of the parameter
   * @param sqlType the SQL type of the parameter
   * @param typeName the type name of the parameter
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public MapSqlParameterSource addValue(String paramName, @Nullable Object value, int sqlType, String typeName) {
    Assert.notNull(paramName, "Parameter name must not be null");
    this.values.put(paramName, value);
    registerSqlType(paramName, sqlType);
    registerTypeName(paramName, typeName);
    return this;
  }

  /**
   * Add a Map of parameters to this parameter source.
   *
   * @param values a Map holding existing parameter values (can be {@code null})
   * @return a reference to this parameter source,
   * so it's possible to chain several calls together
   */
  public MapSqlParameterSource addValues(@Nullable Map<String, ?> values) {
    if (values != null) {
      for (Map.Entry<String, ?> entry : values.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        this.values.put(key, value);
        if (value instanceof SqlParameterValue) {
          registerSqlType(key, ((SqlParameterValue) value).getSqlType());
        }
      }
    }
    return this;
  }

  /**
   * Expose the current parameter values as read-only Map.
   */
  public Map<String, Object> getValues() {
    return Collections.unmodifiableMap(this.values);
  }

  @Override
  public boolean hasValue(String paramName) {
    return this.values.containsKey(paramName);
  }

  @Override
  @Nullable
  public Object getValue(String paramName) {
    if (!hasValue(paramName)) {
      throw new IllegalArgumentException("No value registered for key '" + paramName + "'");
    }
    return this.values.get(paramName);
  }

  @Override
  @NonNull
  public String[] getParameterNames() {
    return StringUtils.toStringArray(this.values.keySet());
  }

}
