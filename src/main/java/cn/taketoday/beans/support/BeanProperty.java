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

package cn.taketoday.beans.support;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.beans.factory.PropertyReadOnlyException;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.AnnotatedElementDecorator;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Field is first considered then readMethod
 *
 * @author TODAY 2021/1/27 22:28
 * @since 3.0
 */
public class BeanProperty extends AnnotatedElementDecorator implements Member, AnnotatedElement, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private Field field;

  private transient BeanInstantiator constructor;
  private transient PropertyAccessor propertyAccessor;

  @Nullable
  private transient Type[] genericClass;

  @Nullable
  private transient Type componentType;
  private boolean componentResolved;
  /** if this property is array or */
  private transient BeanInstantiator componentConstructor;

  @Nullable
  private transient ConversionService conversionService;

  /** @since 3.0.4 */
  @Nullable
  private transient TypeDescriptor typeDescriptor;
  private String alias;

  /** @since 4.0 */
  private final Method readMethod;

  /** @since 4.0 */
  @Nullable
  private final Method writeMethod;

  /** @since 4.0 */
  private Class<?> propertyType;
  /** @since 4.0 */
  private Class<?> declaringClass;

  BeanProperty(String alias, Field field) {
    super(field);
    this.alias = alias;
    this.field = field;
    this.propertyType = field.getType();
    this.readMethod = null;
    this.writeMethod = null;
  }

  BeanProperty(Field field) {
    this(field.getName(), field);
  }

  BeanProperty(Method readMethod, @Nullable Method writeMethod) {
    super(readMethod);
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
  }

  BeanProperty(Method readMethod, @Nullable Method writeMethod, Class<?> declaringClass) {
    super(readMethod);
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = declaringClass;
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
   * @param args arguments objects
   * @return new object
   */
  public Object newInstance(@Nullable Object[] args) {
    BeanInstantiator constructor = this.constructor;
    if (constructor == null) {
      if (ClassUtils.primitiveTypes.contains(propertyType)) {
        throw new BeanInstantiationException(propertyType, "Cannot be instantiated a simple type");
      }
      constructor = BeanInstantiator.fromConstructor(propertyType);
      this.constructor = constructor;
    }
    return constructor.instantiate(args);
  }

  /**
   * new a array object with given length
   */
  public Object newArrayInstance(int length) {
    Class<?> type = this.propertyType;
    if (type.isArray()) {
      type = type.getComponentType();
    }
    return Array.newInstance(type, length);
  }

  public Object getValue(Object object) {
    return obtainAccessor().get(object);
  }

  public Object getValue(Object object, Class<?> requiredType) {
    Object value = getValue(object);
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

  /**
   * @throws PropertyReadOnlyException If this property is read only
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   */
  public final void setValue(Object obj, Object value) {
    if (!getType().isInstance(value)) {
      ConversionService conversionService = getConversionService();
      if (conversionService == null) {
        conversionService = DefaultConversionService.getSharedInstance();
        setConversionService(conversionService);
      }
      TypeDescriptor typeDescriptor = getTypeDescriptor();
      if (typeDescriptor == null) {
        typeDescriptor = TypeDescriptor.fromProperty(this);
        this.typeDescriptor = typeDescriptor;
      }
      value = conversionService.convert(value, typeDescriptor);
    }
    setDirectly(obj, value);
  }

  /**
   * @throws PropertyReadOnlyException If this property is read only
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   * @since 3.0.2
   */
  public final void setDirectly(Object obj, Object value) {
    obtainAccessor().set(obj, value);
  }

  /**
   * @since 3.0.4
   */
  @Nullable
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  // PropertyAccessor

  public PropertyAccessor obtainAccessor() {
    if (propertyAccessor == null) {
      this.propertyAccessor = createAccessor();
    }
    return propertyAccessor;
  }

  /**
   * @since 3.0.2
   */
  protected PropertyAccessor createAccessor() {
    if (field != null) {
      return PropertyAccessor.fromField(field);
    }
    return PropertyAccessor.fromMethod(readMethod, writeMethod);
  }

  /**
   * create a component object without arguments
   *
   * @return component object
   */
  @Nullable
  public Object newComponentInstance() {
    return newComponentInstance(null);
  }

  /**
   * create a component object
   *
   * @return component object
   */
  @Nullable
  public Object newComponentInstance(@Nullable Object[] args) {
    if (componentConstructor == null) {
      Class<?> componentClass = getComponentClass();
      componentConstructor = componentClass == null
                             ? NullInstantiator.INSTANCE
                             : BeanInstantiator.fromConstructor(componentClass);
    }
    return componentConstructor.instantiate(args);
  }

  //
  @Nullable
  public Type[] getGenerics() {
    if (genericClass == null) {
      this.genericClass = ClassUtils.getGenericTypes(field);
    }
    return genericClass;
  }

  @Nullable
  public Type getGeneric(int index) {
    Type[] generics = getGenerics();
    if (generics != null && generics.length > index) {
      return generics[index];
    }
    return null;
  }

  /**
   * Get component {@link Type} may contains generic info
   *
   * @return {@link Type}
   */
  public Type getComponentType() {
    if (componentResolved) {
      return componentType;
    }
    Class<?> type = getType();
    if (type.isArray()) {
      setComponentType(type.getComponentType());
    }
    else if (Map.class.isAssignableFrom(type)) {
      setComponentType(getGeneric(1));
    }
    else {
      setComponentType(getGeneric(0));
    }
    return componentType;
  }

  /**
   * Get component {@link Class}
   *
   * @return {@link Class}
   */
  @Nullable
  public Class<?> getComponentClass() {
    Type componentType = getComponentType();
    if (componentType instanceof Class) {
      return (Class<?>) componentType;
    }
    else if (componentType instanceof ParameterizedType) {
      Type rawType = ((ParameterizedType) componentType).getRawType();
      if (rawType instanceof Class) {
        return (Class<?>) rawType;
      }
    }
    return null;
  }

  public boolean isInstance(Object value) {
    return getType().isInstance(value);
  }

  public BeanInstantiator getConstructor() {
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

  public void setConstructor(BeanInstantiator constructor) {
    this.constructor = constructor;
  }

  public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
    this.propertyAccessor = propertyAccessor;
  }

  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Nullable
  public ConversionService getConversionService() {
    return conversionService;
  }

  public boolean isMap() {
    return Map.class.isAssignableFrom(getType());
  }

  public boolean isList() {
    return List.class.isAssignableFrom(getType());
  }

  public boolean isArray() {
    return getType().isArray();
  }

  public Class<?> getType() {
    if (propertyType == null) {
      Field field = getField();
      if (field != null) {
        propertyType = field.getType();
      }
      else if (readMethod != null) {
        propertyType = readMethod.getReturnType();
      }
      else {
        throw new IllegalStateException("should never get here");
      }
    }
    return propertyType;
  }

  /**
   * get or find a Field
   *
   * @return returns null show that isSynthetic
   */
  @Nullable
  public Field getField() {
    if (field == null && !fieldIsNull) {
      String name = getPropertyName();
      if (StringUtils.isEmpty(name)) {
        return null;
      }
      Class<?> declaringClass = getDeclaringClass();
      if (declaringClass != null) {
        field = ReflectionUtils.findField(declaringClass, name);
        if (field == null) {
          field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
          if (field == null) {
            field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
          }
        }
      }
      fieldIsNull = field == null;
    }
    return field;
  }

  private boolean fieldIsNull;

  /**
   * original property name
   */
  public String getName() {
    if (field != null) {
      return field.getName();
    }
    return getPropertyName();
  }

  @Override
  public int getModifiers() {
    if (field != null) {
      return field.getModifiers();
    }
    return readMethod.getModifiers();
  }

  @Override
  public boolean isSynthetic() {
    Field field = getField();
    if (field != null) {
      return field.isSynthetic();
    }
    return true;
  }

  /**
   * read only
   *
   * @since 3.0.2
   */
  public boolean isReadOnly() {
    if (field != null) {
      return Modifier.isFinal(field.getModifiers());
    }
    return writeMethod == null;
  }

  /**
   * Mapping name
   *
   * @see cn.taketoday.beans.Property
   * @since 4.0
   */
  public String getPropertyName() {
    if (alias == null) {
      alias = ReflectionUtils.getPropertyName(readMethod, writeMethod);
    }
    return alias;
  }

  /**
   * Returns the {@code Class} object representing the class or interface
   * that declares the field represented by this {@code Field} object.
   *
   * @see #getField()
   * @since 4.0
   */
  public Class<?> getDeclaringClass() {
    if (declaringClass == null) {
      if (field == null) {
        declaringClass = readMethod.getDeclaringClass();
      }
      else {
        declaringClass = field.getDeclaringClass();
      }
    }
    return declaringClass;
  }

  @Nullable
  public Method getReadMethod() {
    return readMethod;
  }

  @Nullable
  public Method getWriteMethod() {
    return writeMethod;
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof BeanProperty property) {
      return Objects.equals(field, property.field)
              && Objects.equals(alias, property.alias)
              && Objects.equals(readMethod, property.readMethod)
              && Objects.equals(writeMethod, property.writeMethod)
              && Objects.equals(propertyType, property.propertyType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), field);
  }

  @Override
  public String toString() {
    return getType().getSimpleName() + " " + getName();
  }

  // static

  /**
   * @since 4.0
   */
  public static BeanProperty valueOf(Field field) {
    Assert.notNull(field, "field must not be null");
    return new BeanProperty(field);
  }

  /**
   * @throws NoSuchPropertyException No property in target class
   */
  public static BeanProperty valueOf(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw new NoSuchPropertyException(targetClass, name);
    }
    return new BeanProperty(field);
  }

  /**
   * @param writeMethod can be null (read only)
   */
  public static BeanProperty valueOf(Method readMethod, @Nullable Method writeMethod) {
    return valueOf(readMethod, writeMethod, null);
  }

  /**
   * @param writeMethod can be null (read only)
   * @param declaringClass the implementation class
   */
  public static BeanProperty valueOf(Method readMethod, @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    Assert.notNull(readMethod, "readMethod is required");
    return new BeanProperty(readMethod, writeMethod, declaringClass);
  }

}
