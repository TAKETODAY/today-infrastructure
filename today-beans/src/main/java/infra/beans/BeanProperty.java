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

package infra.beans;

import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import infra.beans.support.BeanInstantiator;
import infra.core.MethodParameter;
import infra.core.conversion.ConversionService;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.reflect.Property;
import infra.reflect.PropertyAccessor;
import infra.reflect.SetterMethod;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

/**
 * Field is first considered then readMethod
 * <p>
 * AnnotatedElement -> Field -> readMethod -> writeMethod
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #isWriteable()
 * @see #isReadable()
 * @since 3.0 2021/1/27 22:28
 */
public sealed class BeanProperty extends Property permits FieldBeanProperty {

  @Serial
  private static final long serialVersionUID = 1L;

  private transient PropertyAccessor accessor;

  private transient BeanInstantiator instantiator;

  BeanProperty(String name, Field field) {
    super(name, field);
    this.field = field;
  }

  BeanProperty(Field field) {
    this(field.getName(), field);
  }

  BeanProperty(@Nullable String name, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    super(name, readMethod, writeMethod, declaringClass);
  }

  BeanProperty(PropertyDescriptor descriptor, Class<?> declaringClass) {
    super(descriptor.getName(), descriptor.getReadMethod(), descriptor.getWriteMethod(), declaringClass);
    if (writeMethod != null && descriptor instanceof GenericTypeAwarePropertyDescriptor generic) {
      this.writeMethodParameter = generic.getWriteMethodParameter();
    }
  }

  /**
   * instantiate property value
   */
  public Object instantiate() {
    return instantiate(null);
  }

  /**
   * instantiate property value
   */
  public Object instantiate(@Nullable Object[] args) {
    BeanInstantiator constructor = this.instantiator;
    if (constructor == null) {
      Class<?> type = getType();
      if (BeanUtils.isSimpleValueType(type)) {
        throw new BeanInstantiationException(type, "Cannot be instantiated a simple type");
      }
      constructor = BeanInstantiator.forConstructor(type);
      this.instantiator = constructor;
    }
    return constructor.instantiate(args);
  }

  /**
   * get property of this {@code object}
   *
   * @param object object
   * @return property value
   */
  public Object getValue(Object object) {
    return obtainAccessor().get(object);
  }

  /**
   * Set bean property
   *
   * @throws NotWritablePropertyException If this property is read only
   * @see SetterMethod#set(Object, Object)
   */
  public final void setValue(Object obj, Object value) {
    value = handleOptional(value, getType());
    setDirectly(obj, value);
  }

  /**
   * Set bean property
   *
   * @throws NotWritablePropertyException If this property is read only
   * @see SetterMethod#set(Object, Object)
   * @since 4.0
   */
  public final void setValue(Object obj, Object value, TypeConverter converter) {
    Class<?> propertyType;
    // write-method parameter type
    MethodParameter writeMethodParameter = getWriteMethodParameter();
    if (writeMethodParameter != null) {
      propertyType = writeMethodParameter.getParameterType();
    }
    else {
      propertyType = getType();
    }
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    else if (!ClassUtils.isAssignableValue(propertyType, value)) {
      Object necessary = converter.convertIfNecessary(value, propertyType, getTypeDescriptor());
      value = handleOptional(necessary, propertyType);
    }
    setDirectly(obj, value);
  }

  /**
   * Set bean property
   *
   * @throws NotWritablePropertyException If this property is read only
   * @see SetterMethod#set(Object, Object)
   * @since 5.0
   */
  public final void setValue(Object obj, Object value, ConversionService conversionService) {
    Class<?> propertyType;
    // write-method parameter type
    MethodParameter writeMethodParameter = getWriteMethodParameter();
    if (writeMethodParameter != null) {
      propertyType = writeMethodParameter.getParameterType();
    }
    else {
      propertyType = getType();
    }
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    else if (!ClassUtils.isAssignableValue(propertyType, value)) {
      Object necessary = conversionService.convert(value, getTypeDescriptor());
      value = handleOptional(necessary, propertyType);
    }
    setDirectly(obj, value);
  }

  // @since 4.0
  @Nullable
  static Object handleOptional(Object value, Class<?> propertyType) {
    // convertedValue == null
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    return value;
  }

  /**
   * @throws NotWritablePropertyException If this property is read only
   * @see SetterMethod#set(Object, Object)
   * @since 3.0.2
   */
  public final void setDirectly(Object obj, Object value) {
    var accessor = obtainAccessor();
    if (accessor.isReadOnly()) {
      throw new NotWritablePropertyException(getDeclaringClass(), getName());
    }
    accessor.set(obj, value);
  }

  // PropertyAccessor

  public final PropertyAccessor obtainAccessor() {
    PropertyAccessor accessor = this.accessor;
    if (accessor == null) {
      accessor = createAccessor();
      this.accessor = accessor;
    }
    return accessor;
  }

  /**
   * @since 3.0.2
   */
  protected PropertyAccessor createAccessor() {
    Field field = getField();
    if (field == null) {
      return PropertyAccessor.forMethod(readMethod, writeMethod);
    }
    return PropertyAccessor.forField(field, readMethod, writeMethod);
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  // static

  /**
   * @since 4.0
   */
  public static BeanProperty valueOf(Field field) {
    Assert.notNull(field, "Field is required");
    return new FieldBeanProperty(field);
  }

  /**
   * @throws NoSuchPropertyException No property in target class
   */
  public static BeanProperty valueOf(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw new NoSuchPropertyException(targetClass, name);
    }
    return new FieldBeanProperty(field);
  }

  /**
   * @param writeMethod can be null (read only)
   */
  public static BeanProperty valueOf(Method readMethod, @Nullable Method writeMethod) {
    return valueOf(readMethod, writeMethod, null);
  }

  /**
   * construct with read-method and write-method
   *
   * @param writeMethod can be null (read only)
   * @param declaringClass the implementation class
   */
  public static BeanProperty valueOf(@Nullable Method readMethod, @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    return valueOf(null, readMethod, writeMethod, declaringClass);
  }

  /**
   * construct with read-method and write-method and property-name
   * <p>
   * <b>NOTE:</b> read-write method cannot be null at the same time
   * </p>
   *
   * @param propertyName user specified property name
   * @param writeMethod can be null (read only)
   * @param declaringClass the implementation class
   */
  public static BeanProperty valueOf(@Nullable String propertyName, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalStateException("Property is neither readable nor writeable");
    }
    return new BeanProperty(propertyName, readMethod, writeMethod, declaringClass);
  }

}
