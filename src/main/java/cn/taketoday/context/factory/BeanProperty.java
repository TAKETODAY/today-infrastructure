/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.factory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.AnnotationSupport;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.exception.BeanInstantiationException;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.NullConstructor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY
 * 2021/1/27 22:28
 * @since 3.0
 */
public class BeanProperty implements AnnotationSupport {

  private final Field field;
  private final Class<?> fieldType;
  private ConstructorAccessor constructor;
  private PropertyAccessor propertyAccessor;

  private Type[] genericClass;

  private Type componentType;
  private boolean componentResolved;
  private ConstructorAccessor componentConstructor;

  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

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
      if (ClassUtils.primitiveTypes.contains(fieldType)) {
        throw new BeanInstantiationException(fieldType, "Cannot be instantiated a simple type");
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

  protected Object convertIfNecessary(final Class<?> requiredType, Object value) {
    if (requiredType.isInstance(value)) {
      return value;
    }
    ConversionService conversionService = getConversionService();
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
      setConversionService(conversionService);
    }
    return conversionService.convert(value, requiredType);
  }

  PropertyAccessor obtainAccessor() {
    if (propertyAccessor == null) {
      propertyAccessor = ReflectionUtils.newPropertyAccessor(field);
    }
    return propertyAccessor;
  }

  public Object newComponentInstance() {
    return newComponentInstance(null);
  }

  public Object newComponentInstance(Object[] args) {
    if (componentConstructor == null) {
      final Class<?> componentClass = getComponentClass();
      componentConstructor = componentClass == null
                             ? NullConstructor.INSTANCE
                             : ReflectionUtils.newConstructorAccessor(componentClass);
    }
    return componentConstructor.newInstance(args);
  }

  //

  public Type[] getGenerics() {
    if (genericClass == null) {
      this.genericClass = ClassUtils.getGenericTypes(field);
    }
    return genericClass;
  }

  public Type getGeneric(final int index) {
    final Type[] generics = getGenerics();
    if (generics != null && generics.length > index) {
      return generics[index];
    }
    return null;
  }

  /**
   * Get componentType
   *
   * @return {@link Type}
   */
  public Type getComponentType() {
    if (componentResolved) {
      return componentType;
    }
    final Class<?> fieldType = this.fieldType;
    if (fieldType.isArray()) {
      setComponentType(fieldType.getComponentType());
    }
    else if (Map.class.isAssignableFrom(fieldType)) {
      setComponentType(getGeneric(1));
    }
    else {
      setComponentType(getGeneric(0));
    }
    return componentType;
  }

  /**
   * Get componentClass
   *
   * @return {@link Class}
   */
  public Class<?> getComponentClass() {
    final Type componentType = getComponentType();
    return componentType instanceof Class ? (Class<?>) componentType : null;
  }

  public ConstructorAccessor getConstructor() {
    return constructor;
  }

  public PropertyAccessor getPropertyAccessor() {
    return propertyAccessor;
  }

  public void setComponentType(Type componentType) {
    this.componentType = componentType;
    if (componentType != null) {
      this.componentResolved = true;
    }
  }

  public void setConstructor(ConstructorAccessor constructor) {
    this.constructor = constructor;
  }

  public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
    this.propertyAccessor = propertyAccessor;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
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

  public String getName() {
    return field.getName();
  }

  // AnnotatedElement

  @Override
  public AnnotatedElement getAnnotationSource() {
    return field;
  }

  // static

  public static BeanProperty of(Class<?> targetClass, String name) {
    final Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw NoSuchPropertyException.noSuchProperty(targetClass, name);
    }
    return new BeanProperty(field);
  }

}
