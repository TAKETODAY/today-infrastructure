/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

/**
 * resolve EnumerationValue
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/2 20:42
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnumerationValueTypeHandler<T extends Enum<T>> implements TypeHandler<T> {
  public static final String fallbackValueBeanPropertyKey = "type-handler.enum-value-property-name";

  private static final String fallbackValueBeanProperty = TodayStrategies.getProperty(
          fallbackValueBeanPropertyKey, "value");

  private final T[] enumConstants;
  private final TypeHandler delegate;
  private final Function<T, Object> valueSupplier;

  public EnumerationValueTypeHandler(Class<T> type, TypeHandlerRegistry registry) {
    Assert.notNull(type, "Type argument is required");
    BeanProperty annotatedProperty = getAnnotatedProperty(type);
    this.enumConstants = type.getEnumConstants();
    if (annotatedProperty == null) {
      // fallback to Enum#name()
      this.valueSupplier = Enum::name;
      this.delegate = registry.getTypeHandler(String.class);
    }
    else {
      this.valueSupplier = annotatedProperty::getValue;
      this.delegate = registry.getTypeHandler(annotatedProperty);
    }
  }

  @Nullable
  static <T> BeanProperty getAnnotatedProperty(Class<T> type) {
    BeanProperty annotatedProperty = null;
    BeanMetadata metadata = BeanMetadata.from(type);
    for (BeanProperty beanProperty : metadata) {
      if (MergedAnnotations.from(beanProperty.getAnnotations()).isPresent(EnumerationValue.class)) {
        Assert.state(annotatedProperty == null, "@EnumerationValue must annotated on one property");
        annotatedProperty = beanProperty;
      }
    }

    if (annotatedProperty == null) {
      // fallback
      annotatedProperty = metadata.getBeanProperty(fallbackValueBeanProperty);
    }
    return annotatedProperty;
  }

  @Override
  public void setParameter(PreparedStatement ps,
          int parameterIndex, @Nullable T parameter) throws SQLException {
    if (parameter != null) {
      Object propertyValue = valueSupplier.apply(parameter);
      delegate.setParameter(ps, parameterIndex, propertyValue);
    }
    else {
      delegate.setParameter(ps, parameterIndex, null);
    }
  }

  @Nullable
  @Override
  public T getResult(ResultSet rs, String columnName) throws SQLException {
    // get value in DB
    Object propertyValueInDb = delegate.getResult(rs, columnName);
    if (propertyValueInDb == null) {
      return null;
    }

    // convert, find target enum
    return convertToEnum(propertyValueInDb);
  }

  @Nullable
  @Override
  public T getResult(ResultSet rs, int columnIndex) throws SQLException {
    // get value in DB
    Object propertyValueInDb = delegate.getResult(rs, columnIndex);
    if (propertyValueInDb == null) {
      return null;
    }

    // convert, find target enum
    return convertToEnum(propertyValueInDb);
  }

  @Nullable
  @Override
  public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
    // get value in DB
    Object propertyValueInDb = delegate.getResult(cs, columnIndex);
    if (propertyValueInDb == null) {
      return null;
    }

    // convert, find target enum
    return convertToEnum(propertyValueInDb);
  }

  //

  @Nullable
  private T convertToEnum(Object propertyValueInDb) {
    for (T enumConstant : enumConstants) {
      Object propertyValue = valueSupplier.apply(enumConstant);
      if (equals(propertyValueInDb, propertyValue)) {
        // found
        return enumConstant;
      }
    }

    return null;
  }

  private static boolean equals(Object propertyValueInDb, Object propertyValue) {
    return Objects.equals(propertyValue, propertyValueInDb);
  }

}
