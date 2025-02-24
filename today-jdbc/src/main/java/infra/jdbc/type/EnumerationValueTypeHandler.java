/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;

/**
 * resolve EnumerationValue
 *
 * @param <T> value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnumerationValue
 * @since 4.0 2022/8/2 20:42
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnumerationValueTypeHandler<T extends Enum<T>> implements TypeHandler<T> {

  public static final String fallbackValueBeanPropertyKey = "jdbc.type-handler.enum-value-property-name";

  private static final String fallbackValueBeanProperty = TodayStrategies.getProperty(fallbackValueBeanPropertyKey, "value");

  private final T[] enumConstants;

  private final TypeHandler delegate;

  private final Function<T, Object> valueSupplier;

  public EnumerationValueTypeHandler(Class<T> type, TypeHandlerManager registry) {
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
    BeanMetadata metadata = BeanMetadata.forClass(type);
    for (BeanProperty beanProperty : metadata) {
      if (beanProperty.mergedAnnotations().isPresent(EnumerationValue.class)) {
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
  public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable T arg) throws SQLException {
    if (arg != null) {
      Object propertyValue = valueSupplier.apply(arg);
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
      if (Objects.equals(propertyValueInDb, propertyValue)) {
        // found
        return enumConstant;
      }
    }

    return null;
  }

}
