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

package cn.taketoday.jdbc.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NullValue;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link RowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class. The mapped target class must be a
 * top-level class or {@code static} nested class, and it may expose either a
 * <em>data class</em> constructor with named parameters corresponding to column
 * names or classic bean property setter methods with property names corresponding
 * to column names or fields with corresponding field names.
 *
 * <p>When combining a data class constructor with setter methods, any property
 * mapped successfully via a constructor argument will not be mapped additionally
 * via a corresponding setter method or field mapping. This means that constructor
 * arguments take precedence over property setter methods which in turn take
 * precedence over direct field mappings.
 *
 * <p>To facilitate mapping between columns and properties that don't have matching
 * names, try using underscore-separated column aliases in the SQL statement like
 * {@code "select fname as first_name from customer"}, where {@code first_name}
 * can be mapped to a {@code setFirstName(String)} method in the target class.
 *
 * <p>This is a flexible alternative to {@link DataClassRowMapper} and
 * {@link BeanPropertyRowMapper} for scenarios where no specific customization
 * and no pre-defined property mappings are needed.
 *
 * <p>In terms of its fallback property discovery algorithm, this class is similar to
 * {@link cn.taketoday.jdbc.core.namedparam.SimplePropertySqlParameterSource}
 * and is similarly used for {@link cn.taketoday.jdbc.core.simple.JdbcClient}.
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DataClassRowMapper
 * @see BeanPropertyRowMapper
 * @see cn.taketoday.jdbc.core.simple.JdbcClient.StatementSpec#query(Class)
 * @see cn.taketoday.jdbc.core.namedparam.SimplePropertySqlParameterSource
 * @since 4.0
 */
public class SimplePropertyRowMapper<T> implements RowMapper<T> {

  private final Class<T> mappedClass;

  private final ConversionService conversionService;

  private final Constructor<T> mappedConstructor;

  private final String[] constructorParameterNames;

  private final TypeDescriptor[] constructorParameterTypes;

  private final Map<String, Object> propertyDescriptors = new ConcurrentHashMap<>();

  /**
   * Create a new {@code SimplePropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  public SimplePropertyRowMapper(Class<T> mappedClass) {
    this(mappedClass, DefaultConversionService.getSharedInstance());
  }

  /**
   * Create a new {@code SimplePropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param conversionService a {@link ConversionService} for binding
   * JDBC values to bean properties
   */
  public SimplePropertyRowMapper(Class<T> mappedClass, ConversionService conversionService) {
    Assert.notNull(mappedClass, "Mapped Class is required");
    Assert.notNull(conversionService, "ConversionService is required");
    this.mappedClass = mappedClass;
    this.conversionService = conversionService;

    this.mappedConstructor = BeanUtils.obtainConstructor(mappedClass);
    int paramCount = this.mappedConstructor.getParameterCount();
    this.constructorParameterNames = paramCount > 0
                                     ? BeanUtils.getParameterNames(this.mappedConstructor)
                                     : Constant.EMPTY_STRING_ARRAY;
    this.constructorParameterTypes = new TypeDescriptor[paramCount];
    for (int i = 0; i < paramCount; i++) {
      this.constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(this.mappedConstructor, i));
    }
  }

  @Override
  public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
    Object[] args = new Object[this.constructorParameterNames.length];
    Set<Integer> usedIndex = new HashSet<>();
    for (int i = 0; i < args.length; i++) {
      String name = this.constructorParameterNames[i];
      int index;
      try {
        // Try direct name match first
        index = rs.findColumn(name);
      }
      catch (SQLException ex) {
        // Try underscored name match instead
        index = rs.findColumn(JdbcUtils.convertPropertyNameToUnderscoreName(name));
      }
      TypeDescriptor td = this.constructorParameterTypes[i];
      Object value = JdbcUtils.getResultSetValue(rs, index, td.getType());
      usedIndex.add(index);
      args[i] = this.conversionService.convert(value, td);
    }
    T mappedObject = BeanUtils.newInstance(this.mappedConstructor, args);

    ResultSetMetaData rsmd = rs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    for (int index = 1; index <= columnCount; index++) {
      if (!usedIndex.contains(index)) {
        Object desc = getDescriptor(JdbcUtils.lookupColumnName(rsmd, index));
        if (desc instanceof MethodParameter mp) {
          Method method = mp.getMethod();
          if (method != null) {
            Object value = JdbcUtils.getResultSetValue(rs, index, mp.getParameterType());
            value = this.conversionService.convert(value, new TypeDescriptor(mp));
            ReflectionUtils.makeAccessible(method);
            ReflectionUtils.invokeMethod(method, mappedObject, value);
          }
        }
        else if (desc instanceof Field field) {
          Object value = JdbcUtils.getResultSetValue(rs, index, field.getType());
          value = this.conversionService.convert(value, new TypeDescriptor(field));
          ReflectionUtils.makeAccessible(field);
          ReflectionUtils.setField(field, mappedObject, value);
        }
      }
    }

    return mappedObject;
  }

  private Object getDescriptor(String column) {
    return this.propertyDescriptors.computeIfAbsent(column, name -> {
      // Try direct match first
      PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.mappedClass, name);
      if (pd != null && pd.getWriteMethod() != null) {
        return BeanUtils.getWriteMethodParameter(pd);
      }
      Field field = ReflectionUtils.findField(this.mappedClass, name);
      if (field != null) {
        return field;
      }

      // Try de-underscored match instead
      String adaptedName = JdbcUtils.convertUnderscoreNameToPropertyName(name);
      if (!adaptedName.equals(name)) {
        pd = BeanUtils.getPropertyDescriptor(this.mappedClass, adaptedName);
        if (pd != null && pd.getWriteMethod() != null) {
          return BeanUtils.getWriteMethodParameter(pd);
        }
        field = ReflectionUtils.findField(this.mappedClass, adaptedName);
        if (field != null) {
          return field;
        }
      }

      // Fallback: case-insensitive match
      PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(this.mappedClass);
      for (PropertyDescriptor candidate : pds) {
        if (name.equalsIgnoreCase(candidate.getName())) {
          return BeanUtils.getWriteMethodParameter(candidate);
        }
      }
      field = ReflectionUtils.findFieldIgnoreCase(this.mappedClass, name);
      if (field != null) {
        return field;
      }

      return NullValue.INSTANCE;
    });
  }

}
