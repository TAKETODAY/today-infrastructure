/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.core;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import infra.beans.BeanMetadata;
import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.beans.support.BeanInstantiator;
import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.jdbc.support.JdbcUtils;
import infra.util.Assert;
import infra.logging.LoggerFactory;

/**
 * {@link RowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class. The mapped target class must be a
 * top-level class and may either expose a data class constructor with named
 * parameters corresponding to column names or classic bean property setters
 * (or even a combination of both).
 *
 * <p>Note that this class extends {@link BeanPropertyRowMapper} and can
 * therefore serve as a common choice for any mapped target class, flexibly
 * adapting to constructor style versus setter methods in the mapped class.
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimplePropertyRowMapper
 * @since 4.0
 */
public class DataClassRowMapper<T extends @Nullable Object> extends BeanPropertyRowMapper<T> {

  @Nullable
  private BeanInstantiator mappedInstantiator;

  private String @Nullable [] constructorParameterNames;

  private TypeDescriptor @Nullable [] constructorParameterTypes;

  /** Map from underscored parameter names to constructor parameter indexes. */
  private @Nullable Map<String, Integer> underscoredParameterIndexes;

  /** Map from lower-case parameter names to constructor parameter indexes. */
  private @Nullable Map<String, Integer> lowerCaseParameterIndexes;

  /**
   * Create a new {@code DataClassRowMapper} for bean-style configuration.
   *
   * @see #setMappedClass
   * @see #setConversionService
   */
  public DataClassRowMapper() {
  }

  /**
   * Create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  public DataClassRowMapper(Class<T> mappedClass) {
    super(mappedClass);
  }

  @Override
  protected void initialize(Class<T> mappedClass, BeanMetadata metadata) {
    super.initialize(mappedClass, metadata);
    this.mappedInstantiator = metadata.getInstantiator();
    Constructor<?> constructor = mappedInstantiator.getConstructor();
    if (constructor != null) {
      int paramCount = constructor.getParameterCount();
      if (paramCount > 0) {
        String[] constructorParameterNames = BeanUtils.getParameterNames(constructor);
        this.underscoredParameterIndexes = new HashMap<>(paramCount);
        this.lowerCaseParameterIndexes = new HashMap<>(paramCount);
        for (int i = 0; i < constructorParameterNames.length; i++) {
          String name = constructorParameterNames[i];
          suppressProperty(name);
          this.underscoredParameterIndexes.putIfAbsent(underscoreName(name), i);
          this.lowerCaseParameterIndexes.putIfAbsent(lowerCaseName(name), i);
        }
        TypeDescriptor[] constructorParameterTypes = new TypeDescriptor[constructorParameterNames.length];
        for (int i = 0; i < constructorParameterTypes.length; i++) {
          constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(constructor, i));
        }
        this.constructorParameterTypes = constructorParameterTypes;
        this.constructorParameterNames = constructorParameterNames;
      }
    }
    else {
      LoggerFactory.getLogger(DataClassRowMapper.class)
              .warn("Actual 'java.lang.reflect.Constructor' cannot determine in mappedClass: '{}'", mappedClass.getName());
    }
  }

  @Override
  @SuppressWarnings("NullAway")
  protected T constructMappedInstance(ResultSet rs, TypeConverter converter) throws SQLException {
    BeanInstantiator mappedConstructor = this.mappedInstantiator;
    Assert.state(mappedConstructor != null, "Mapped constructor was not initialized");
    String[] constructorParameterNames = this.constructorParameterNames;
    TypeDescriptor[] constructorParameterTypes = this.constructorParameterTypes;

    Object[] args = null;
    if (constructorParameterNames != null && constructorParameterTypes != null) {
      args = new Object[constructorParameterNames.length];
      int[] columnIndexes = findColumnIndexes(rs, constructorParameterNames);
      for (int i = 0; i < args.length; i++) {
        TypeDescriptor td = constructorParameterTypes[i];
        Object value = getColumnValue(rs, columnIndexes[i], td.getType());
        args[i] = converter.convertIfNecessary(value, td.getType(), td);
      }
    }

    return BeanUtils.newInstance(mappedConstructor, args);
  }

  /**
   * Determine the column index for each constructor parameter from the
   * column labels of the given {@code ResultSet}, trying the common
   * underscored name match first and a direct name match (typically with
   * camelCase) second. For duplicate labels, the first column is used in
   * line with {@link ResultSet#findColumn}.
   */
  private int[] findColumnIndexes(ResultSet rs, String[] parameterNames) throws SQLException {
    Map<String, Integer> underscoredIndexes = this.underscoredParameterIndexes;
    Map<String, Integer> lowerCaseIndexes = this.lowerCaseParameterIndexes;
    Assert.state(underscoredIndexes != null && lowerCaseIndexes != null,
            "Parameter indexes were not initialized");
    int[] underscoredMatches = new int[parameterNames.length];
    int[] directMatches = new int[parameterNames.length];
    ResultSetMetaData rsmd = rs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    for (int index = 1; index <= columnCount; index++) {
      String column = lowerCaseName(JdbcUtils.lookupColumnName(rsmd, index));
      Integer param = underscoredIndexes.get(column);
      if (param != null && underscoredMatches[param] == 0) {
        underscoredMatches[param] = index;
      }
      param = lowerCaseIndexes.get(column);
      if (param != null && directMatches[param] == 0) {
        directMatches[param] = index;
      }
    }
    for (int i = 0; i < parameterNames.length; i++) {
      if (underscoredMatches[i] == 0) {
        underscoredMatches[i] = directMatches[i] != 0 ? directMatches[i] : findColumn(rs, parameterNames[i]);
      }
    }
    return underscoredMatches;
  }

  /**
   * Fall back to {@link ResultSet#findColumn} for a constructor parameter
   * without a matching column label, exposing the driver's exception
   * if the column cannot be found.
   */
  private int findColumn(ResultSet rs, String name) throws SQLException {
    try {
      // Try common underscored name match first
      return rs.findColumn(underscoreName(name));
    }
    catch (SQLException ex) {
      // Try direct name match (typically with camelCase) instead
      return rs.findColumn(lowerCaseName(name));
    }
  }

}
