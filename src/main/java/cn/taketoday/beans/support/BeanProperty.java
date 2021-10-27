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

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.beans.factory.PropertyReadOnlyException;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.AbstractAnnotatedElement;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.Mappings;
import cn.taketoday.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author TODAY
 * 2021/1/27 22:28
 * @since 3.0
 */
public class BeanProperty extends AbstractAnnotatedElement {
  private static final long serialVersionUID = 1L;

  private static final Mappings<Annotation[], BeanProperty> annotationsCache = new Mappings<>();

  private final Field field;
  private final Class<?> fieldType;
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

  private transient Annotation[] annotations;

  /** @since 3.0.4 */
  private transient TypeDescriptor typeDescriptor;
  private final String alias;

  BeanProperty(String alias, Field field) {
    this.alias = alias;
    this.field = field;
    this.fieldType = field.getType();
  }

  BeanProperty(Field field) {
    this(field.getName(), field);
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
      Class<?> fieldType = this.fieldType;
      if (ClassUtils.primitiveTypes.contains(fieldType)) {
        throw new BeanInstantiationException(fieldType, "Cannot be instantiated a simple type");
      }
      constructor = BeanInstantiator.fromConstructor(fieldType);
      this.constructor = constructor;
    }
    return constructor.instantiate(args);
  }

  /**
   * new a array object with given length
   */
  public Object newArrayInstance(int length) {
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
    if (!fieldType.isInstance(value)) {
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
    return PropertyAccessor.fromField(field);
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
    return fieldType.isInstance(value);
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

  /**
   * original property name
   */
  public String getName() {
    return field.getName();
  }

  /**
   * @since 3.0.2
   */
  public boolean isReadOnly() {
    return Modifier.isFinal(field.getModifiers());
  }

  /**
   * Mapping name
   *
   * @see cn.taketoday.beans.Property
   * @since 4.0
   */
  public String getPropertyName() {
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
    return field.getDeclaringClass();
  }

  //---------------------------------------------------------------------
  // Implementation of AnnotatedElement interface
  //---------------------------------------------------------------------

  @Override
  public Annotation[] getAnnotations() {
    if (annotations == null) {
      annotations = resolveAnnotations();
    }
    return annotations;
  }

  private Annotation[] resolveAnnotations() {
    return annotationsCache.get(this, k -> {
      ArrayList<Annotation> annotations = new ArrayList<>();
      Method readMethod = obtainAccessor().getReadMethod();
      if (!((BeanProperty) k).isReadOnly()) {
        Method writeMethod = obtainAccessor().getWriteMethod();
        if (writeMethod != null) {
          Collections.addAll(annotations, writeMethod.getAnnotations());
        }
      }
      if (readMethod != null) {
        Collections.addAll(annotations, readMethod.getAnnotations());
      }
      Collections.addAll(annotations, field.getAnnotations());
      return annotations.toArray(Constant.EMPTY_ANNOTATION_ARRAY);
    });
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanProperty))
      return false;
    if (!super.equals(o))
      return false;
    BeanProperty that = (BeanProperty) o;
    return Objects.equals(field, that.field);
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

}
