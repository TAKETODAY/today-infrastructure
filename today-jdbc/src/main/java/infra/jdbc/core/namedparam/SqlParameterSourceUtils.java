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

package infra.jdbc.core.namedparam;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import infra.jdbc.core.SqlParameterValue;
import infra.lang.Nullable;

/**
 * Class that provides helper methods for the use of {@link SqlParameterSource},
 * in particular with {@link NamedParameterJdbcTemplate}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public abstract class SqlParameterSourceUtils {

  /**
   * Create an array of {@link SqlParameterSource} objects populated with data
   * from the values passed in (either a {@link Map} or a bean object).
   * This will define what is included in a batch operation.
   *
   * @param candidates object array of objects containing the values to be used
   * @return an array of {@link SqlParameterSource}
   * @see MapSqlParameterSource
   * @see BeanPropertySqlParameterSource
   * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
   */
  public static SqlParameterSource[] createBatch(Object... candidates) {
    return createBatch(Arrays.asList(candidates));
  }

  /**
   * Create an array of {@link SqlParameterSource} objects populated with data
   * from the values passed in (either a {@link Map} or a bean object).
   * This will define what is included in a batch operation.
   *
   * @param candidates collection of objects containing the values to be used
   * @return an array of {@link SqlParameterSource}
   * @see MapSqlParameterSource
   * @see BeanPropertySqlParameterSource
   * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
   */
  @SuppressWarnings("unchecked")
  public static SqlParameterSource[] createBatch(Collection<?> candidates) {
    SqlParameterSource[] batch = new SqlParameterSource[candidates.size()];
    int i = 0;
    for (Object candidate : candidates) {
      batch[i] = (candidate instanceof Map ? new MapSqlParameterSource((Map<String, ?>) candidate) :
              new BeanPropertySqlParameterSource(candidate));
      i++;
    }
    return batch;
  }

  /**
   * Create an array of {@link MapSqlParameterSource} objects populated with data from
   * the values passed in. This will define what is included in a batch operation.
   *
   * @param valueMaps array of {@link Map} instances containing the values to be used
   * @return an array of {@link SqlParameterSource}
   * @see MapSqlParameterSource
   * @see NamedParameterJdbcTemplate#batchUpdate(String, Map[])
   */
  public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
    SqlParameterSource[] batch = new SqlParameterSource[valueMaps.length];
    for (int i = 0; i < valueMaps.length; i++) {
      batch[i] = new MapSqlParameterSource(valueMaps[i]);
    }
    return batch;
  }

  /**
   * Create a wrapped value if parameter has type information, plain object if not.
   *
   * @param source the source of parameter values and type information
   * @param parameterName the name of the parameter
   * @return the value object
   * @see SqlParameterValue
   */
  @Nullable
  public static Object getTypedValue(SqlParameterSource source, String parameterName) {
    int sqlType = source.getSqlType(parameterName);
    if (sqlType != SqlParameterSource.TYPE_UNKNOWN) {
      return new SqlParameterValue(sqlType, source.getTypeName(parameterName), source.getValue(parameterName));
    }
    else {
      return source.getValue(parameterName);
    }
  }

  /**
   * Create a Map of case insensitive parameter names together with the original name.
   *
   * @param parameterSource the source of parameter names
   * @return the Map that can be used for case insensitive matching of parameter names
   */
  public static Map<String, String> extractCaseInsensitiveParameterNames(SqlParameterSource parameterSource) {
    HashMap<String, String> caseInsensitiveParameterNames = new HashMap<>();
    String[] paramNames = parameterSource.getParameterNames();
    if (paramNames != null) {
      for (String name : paramNames) {
        caseInsensitiveParameterNames.put(name.toLowerCase(Locale.ROOT), name);
      }
    }
    return caseInsensitiveParameterNames;
  }

}
