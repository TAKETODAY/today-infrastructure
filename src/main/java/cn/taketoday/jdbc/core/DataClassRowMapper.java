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

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
 * @since 4.0
 */
public class DataClassRowMapper<T> extends BeanPropertyRowMapper<T> {

  @Nullable
  private Constructor<T> mappedConstructor;

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
  protected void initialize(Class<T> mappedClass) {
    super.initialize(mappedClass);

    this.mappedConstructor = BeanUtils.getResolvableConstructor(mappedClass);
    int paramCount = this.mappedConstructor.getParameterCount();
    if (paramCount > 0) {
      this.constructorParameterNames = BeanUtils.getParameterNames(this.mappedConstructor);
      for (String name : this.constructorParameterNames) {
        suppressProperty(name);
      }
      this.constructorParameterTypes = new TypeDescriptor[paramCount];
      for (int i = 0; i < paramCount; i++) {
        this.constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(this.mappedConstructor, i));
      }
    }
  }

  @Override
  protected T constructMappedInstance(ResultSet rs, TypeConverter tc) throws SQLException {
    Assert.state(this.mappedConstructor != null, "Mapped constructor was not initialized");

    Object[] args;
    if (this.constructorParameterNames != null && this.constructorParameterTypes != null) {
      args = new Object[this.constructorParameterNames.length];
      for (int i = 0; i < args.length; i++) {
        String name = underscoreName(this.constructorParameterNames[i]);
        TypeDescriptor td = this.constructorParameterTypes[i];
        Object value = getColumnValue(rs, rs.findColumn(name), td.getType());
        args[i] = tc.convertIfNecessary(value, td.getType(), td);
      }
    }
    else {
      args = new Object[0];
    }

    return BeanUtils.newInstance(this.mappedConstructor, args);
  }

  /**
   * Static factory method to create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @see #newInstance(Class, ConversionService)
   */
  public static <T> DataClassRowMapper<T> newInstance(Class<T> mappedClass) {
    return new DataClassRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param conversionService the {@link ConversionService} for binding
   * JDBC values to bean properties, or {@code null} for none
   * @see #newInstance(Class)
   * @see #setConversionService
   */
  public static <T> DataClassRowMapper<T> newInstance(
          Class<T> mappedClass, @Nullable ConversionService conversionService) {

    DataClassRowMapper<T> rowMapper = newInstance(mappedClass);
    rowMapper.setConversionService(conversionService);
    return rowMapper;
  }

}
