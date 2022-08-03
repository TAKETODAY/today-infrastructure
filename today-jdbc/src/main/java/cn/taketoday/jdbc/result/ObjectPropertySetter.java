/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * for any pojo
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setTo(Object, ResultSet, int)
 * @since 2021/1/7 22:49
 */
public class ObjectPropertySetter {

  @Nullable
  private final PropertyPath propertyPath;
  private final BeanProperty beanProperty; // cache

  private final TypeHandler<?> typeHandler;
  private final ConversionService conversionService;

  @Nullable
  private final PrimitiveTypeNullHandler primitiveTypeNullHandler;

  public ObjectPropertySetter(
          @Nullable PropertyPath propertyPath, BeanProperty beanProperty, RepositoryManager manager) {
    this(propertyPath, beanProperty,
            manager.getConversionService(),
            manager.getTypeHandler(beanProperty),
            manager.getPrimitiveTypeNullHandler());
  }

  public ObjectPropertySetter(
          @Nullable PropertyPath propertyPath, BeanProperty beanProperty,
          ConversionService conversionService, TypeHandler<?> typeHandler,
          @Nullable PrimitiveTypeNullHandler primitiveTypeNullHandler) {
    Assert.notNull(typeHandler, "TypeHandler is required");
    Assert.notNull(beanProperty, "BeanProperty is required");
    Assert.notNull(conversionService, "ConversionService is required");
    this.typeHandler = typeHandler;
    this.propertyPath = propertyPath;
    this.beanProperty = beanProperty;
    this.conversionService = conversionService;
    this.primitiveTypeNullHandler = primitiveTypeNullHandler;
  }

  /**
   * Set the data to {@code obj} from given {@code columnIndex} and {@link ResultSet}
   *
   * @param obj object to set
   * @param resultSet jdbc resultSet
   * @param columnIndex current column index
   * @throws SQLException when data fetch failed
   */
  public void setTo(Object obj, ResultSet resultSet, int columnIndex) throws SQLException {
    Object result = getResult(resultSet, columnIndex);
    if (result == null && beanProperty.isPrimitive()) {
      if (primitiveTypeNullHandler != null) {
        if (propertyPath != null) {
          obj = propertyPath.getNestedObject(obj);
        }
        primitiveTypeNullHandler.handleNull(beanProperty, obj);
      }
    }
    else {
      if (propertyPath != null) {
        propertyPath.set(obj, result);
      }
      else {
        beanProperty.setValue(obj, result);
      }
    }
  }

  /**
   * Get result from {@link ResultSet}.
   * <p>
   * Obtain from {@link TypeHandler}, if it fails, use the default acquisition method
   * </p>
   *
   * @param resultSet Target result set
   * @return data object
   * @throws SQLException If {@link ResultSet#getObject(int)} failed
   */
  @Nullable
  private Object getResult(ResultSet resultSet, int columnIndex) throws SQLException {
    try {
      return typeHandler.getResult(resultSet, columnIndex);
    }
    catch (SQLException e) {
      // maybe data conversion error
      Object object = resultSet.getObject(columnIndex);
      if (object == null || beanProperty.isInstance(object)) {
        return object;
      }
      try {
        return conversionService.convert(object, beanProperty.getTypeDescriptor());
      }
      catch (ConversionException ex) {
        if (ex.getCause() instanceof IllegalArgumentException cause) {
          String message = cause.getMessage();
          if ("A null value cannot be assigned to a primitive type".equals(message)) {
            // primitive type
            return null;
          }
        }
        e.addSuppressed(ex);
        throw e;
      }
    }
  }

}
