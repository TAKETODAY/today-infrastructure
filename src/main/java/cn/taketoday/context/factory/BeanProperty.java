/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package cn.taketoday.context.factory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.exception.BeanInstantiationException;
import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY
 * @date 2021/1/27 22:28
 * @since 3.0
 */
public class BeanProperty {
  private final Field field;
  private final Class<?> fieldType;
  private ConstructorAccessor constructor;
  private PropertyAccessor propertyAccessor;

  public BeanProperty(Field field) {
    Assert.notNull(field, "field must not be null");
    this.field = field;
    this.fieldType = field.getType();
  }

  /**
   * invoke default constructor with none arguments
   *
   * @return new object
   */
  public Object newInstance() {
    return newInstance(null);
  }

  /**
   * invoke constructor with given arguments
   *
   * @param args
   *         arguments objects
   *
   * @return new object
   */
  public Object newInstance(final Object[] args) {
    if (constructor == null) {
      final Class<?> fieldType = this.fieldType;
      if (ClassUtils.isSimpleType(fieldType)) {
        throw new BeanInstantiationException(fieldType, "Cannot be instantiated a simple type");
      }
      if (fieldType.isArray()) {
        return Array.newInstance(fieldType.getComponentType(), 1);
      }
      constructor = ReflectionUtils.newConstructorAccessor(fieldType);
    }
    return constructor.newInstance(args);
  }

  /**
   * new a array object with given length
   */
  public Object newArrayInstance(final int length) {
    Class<?> type = this.fieldType;
    if (type.isArray()) {
      type = type.getComponentType();
    }
    return Array.newInstance(type, length);
  }

  public Object getValue(Object object) {
    return obtainAccessor().get(object);
  }

  public Object getValue(Object object, Class<?> requiredType) {
    return convertIfNecessary(requiredType, getValue(object));
  }

  public void setValue(Object obj, Object value) {
    obtainAccessor().set(obj, convertIfNecessary(fieldType, value));
  }

  final Object convertIfNecessary(final Class<?> requiredType, Object value) {
    return requiredType.isInstance(value) ? value : ConvertUtils.convert(value, requiredType);
  }

  PropertyAccessor obtainAccessor() {
    if (propertyAccessor == null) {
      propertyAccessor = ReflectionUtils.newPropertyAccessor(field);
    }
    return propertyAccessor;
  }

  //

  public ConstructorAccessor getConstructor() {
    return constructor;
  }

  public void setConstructor(ConstructorAccessor constructor) {
    this.constructor = constructor;
  }

  public PropertyAccessor getPropertyAccessor() {
    return propertyAccessor;
  }

  public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
    this.propertyAccessor = propertyAccessor;
  }

  public boolean isMap() {
    return Map.class.isAssignableFrom(fieldType);
  }

  public boolean isList() {
    return List.class.isAssignableFrom(fieldType);
  }

  public boolean isArray() {
    return fieldType.isArray();
  }

  public Class<?> getType() {
    return fieldType;
  }

  public Field getField() {
    return field;
  }
}
