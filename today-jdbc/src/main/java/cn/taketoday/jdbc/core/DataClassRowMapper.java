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

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;

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
public class DataClassRowMapper<T> extends BeanPropertyRowMapper<T> {

  @Nullable
  private BeanInstantiator mappedInstantiator;

  @Nullable
  private String[] constructorParameterNames;

  @Nullable
  private TypeDescriptor[] constructorParameterTypes;

  /**
   * Create a new {@code DataClassRowMapper} for bean-style configuration.
   *
   * @see #setMappedClass
   * @see #setConversionService
   */
  public DataClassRowMapper() { }

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
        for (String name : constructorParameterNames) {
          suppressProperty(name);
        }
        TypeDescriptor[] constructorParameterTypes = new TypeDescriptor[paramCount];
        for (int i = 0; i < paramCount; i++) {
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
  protected T constructMappedInstance(ResultSet rs, TypeConverter converter) throws SQLException {
    BeanInstantiator mappedConstructor = this.mappedInstantiator;
    Assert.state(mappedConstructor != null, "Mapped constructor was not initialized");
    String[] constructorParameterNames = this.constructorParameterNames;
    TypeDescriptor[] constructorParameterTypes = this.constructorParameterTypes;

    Object[] args = null;
    if (constructorParameterNames != null && constructorParameterTypes != null) {
      args = new Object[constructorParameterNames.length];
      int i = 0;
      for (String name : constructorParameterNames) {
        TypeDescriptor td = constructorParameterTypes[i];
        int index;
        try {
          // Try direct name match first
          index = rs.findColumn(name);
        }
        catch (SQLException ex) {
          try {
            // Try underscored name match instead
            index = rs.findColumn(underscoreName(name));
          }
          catch (SQLException e) {
            index = rs.findColumn(lowerCaseName(name));
          }
        }

        Object value = getColumnValue(rs, index, td.getType());
        args[i++ /* plus 1 */] = converter.convertIfNecessary(value, td.getType(), td);
      }
    }

    return BeanUtils.newInstance(mappedConstructor, args);
  }

  /**
   * Static factory method to create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @see #forClass(Class, ConversionService)
   */
  public static <T> DataClassRowMapper<T> forClass(Class<T> mappedClass) {
    return new DataClassRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param conversionService the {@link ConversionService} for binding
   * JDBC values to bean properties, or {@code null} for none
   * @see #forClass(Class)
   * @see #setConversionService
   */
  public static <T> DataClassRowMapper<T> forClass(
          Class<T> mappedClass, @Nullable ConversionService conversionService) {

    DataClassRowMapper<T> rowMapper = forClass(mappedClass);
    rowMapper.setConversionService(conversionService);
    return rowMapper;
  }

}
