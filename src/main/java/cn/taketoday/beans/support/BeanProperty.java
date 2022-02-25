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

package cn.taketoday.beans.support;

import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.NotWritablePropertyException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Field is first considered then readMethod
 * <p>
 * AnnotatedElement -> Field -> readMethod -> writeMethod
 *
 * @author TODAY 2021/1/27 22:28
 * @see #isWriteable()
 * @see #isReadable()
 * @since 3.0
 */
public sealed class BeanProperty implements Member, AnnotatedElement, Serializable permits FieldBeanProperty {
  @Serial
  private static final long serialVersionUID = 1L;

  private static final ConcurrentReferenceHashMap<BeanProperty, Annotation[]> annotationCache = new ConcurrentReferenceHashMap<>();

  // Nullable
  protected transient Field field;

  private transient BeanInstantiator constructor;
  private transient PropertyAccessor propertyAccessor;

  @Nullable
  private transient Type componentType;
  private boolean componentResolved;
  /** if this property is array or */
  private transient BeanInstantiator componentConstructor;

  /** @since 3.0.4 */
  @Nullable
  private transient TypeDescriptor typeDescriptor;

  private final String name;

  /** @since 4.0 */
  @Nullable
  private final Method readMethod;

  /** @since 4.0 */
  @Nullable
  private final Method writeMethod;

  /** @since 4.0 */
  private Class<?> propertyType;
  /** @since 4.0 */
  private Class<?> declaringClass;
  /** @since 4.0 */
  private boolean fieldIsNull;

  @Nullable
  private transient Annotation[] annotations;

  // @since 4.0
  private ResolvableType resolvableType;

  // @since 4.0
  private transient MethodParameter methodParameter;

  // @since 4.0
  private transient MethodParameter writeMethodParameter;

  BeanProperty(String name, Field field) {
    this.name = name;
    this.field = field;
    this.propertyType = field.getType();
    this.readMethod = null;
    this.writeMethod = null;
  }

  BeanProperty(Field field) {
    this(field.getName(), field);
  }

  BeanProperty(@Nullable String name,
               @Nullable Method readMethod,
               @Nullable Method writeMethod,
               @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalArgumentException("Property '" + name + "' in '" + declaringClass + "' is neither readable nor writeable");
    }
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = declaringClass;
    if (name == null) {
      name = ReflectionUtils.getPropertyName(readMethod, writeMethod);
    }
    this.name = name;
  }

  BeanProperty(PropertyDescriptor descriptor, Class<?> declaringClass) {
    this.name = descriptor.getName();
    this.declaringClass = declaringClass;
    this.readMethod = descriptor.getReadMethod();
    this.writeMethod = descriptor.getWriteMethod();
    this.propertyType = descriptor.getPropertyType();
    this.writeMethodParameter = BeanUtils.getWriteMethodParameter(descriptor);
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
      if (BeanUtils.isSimpleValueType(propertyType)) {
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

  /**
   * @throws NotWritablePropertyException If this property is read only
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   */
  public final void setValue(Object obj, Object value) {
    value = handleOptional(value, getType());
    setDirectly(obj, value);
  }

  /**
   * @throws NotWritablePropertyException If this property is read only
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   * @since 4.0
   */
  public final void setValue(Object obj, Object value, TypeConverter converter) {
    Class<?> propertyType = getType();
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    else if (!ClassUtils.isAssignableValue(propertyType, value)) {
      Object necessary = converter.convertIfNecessary(value, propertyType, getTypeDescriptor());
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
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   * @since 3.0.2
   */
  public final void setDirectly(Object obj, Object value) {
    obtainAccessor().set(obj, value);
  }

  /**
   * Returns {@link TypeDescriptor} for this property
   *
   * @since 3.0.4
   */
  public final TypeDescriptor getTypeDescriptor() {
    TypeDescriptor typeDescriptor = this.typeDescriptor;
    if (typeDescriptor == null) {
      typeDescriptor = createDescriptor();
      this.typeDescriptor = typeDescriptor;
    }
    return typeDescriptor;
  }

  protected TypeDescriptor createDescriptor() {
    ResolvableType resolvableType = ResolvableType.forMethodParameter(getMethodParameter());
    return new TypeDescriptor(resolvableType, resolvableType.resolve(getType()), this);
  }

  public ResolvableType getResolvableType() {
    ResolvableType resolvableType = this.resolvableType;
    if (resolvableType == null) {
      resolvableType = createResolvableType();
      this.resolvableType = resolvableType;
    }
    return resolvableType;
  }

  protected ResolvableType createResolvableType() {
    Method readMethod = getReadMethod();
    if (readMethod != null) {
      return ResolvableType.forReturnType(readMethod, getDeclaringClass());
    }
    Method writeMethod = getWriteMethod();
    if (writeMethod != null) {
      return ResolvableType.forParameter(writeMethod, 0, getDeclaringClass());
    }
    throw new IllegalStateException("never get here");
  }

  // PropertyAccessor

  public final PropertyAccessor obtainAccessor() {
    PropertyAccessor accessor = this.propertyAccessor;
    if (accessor == null) {
      accessor = createAccessor();
      this.propertyAccessor = accessor;
    }
    return accessor;
  }

  /**
   * @since 3.0.2
   */
  protected PropertyAccessor createAccessor() {
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
    BeanInstantiator constructor = this.componentConstructor;
    if (constructor == null) {
      Class<?> componentClass = getComponentClass();
      constructor = componentClass == null
                    ? NullInstantiator.INSTANCE
                    : BeanInstantiator.fromConstructor(componentClass);
      this.componentConstructor = constructor;
    }
    return constructor.instantiate(args);
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
      setComponentType(getResolvableType().resolveGeneric(1));
    }
    else {
      setComponentType(getResolvableType().resolveGeneric(0));
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

  public Class<?> getType() {
    if (propertyType == null) {
      if (readMethod != null) {
        propertyType = readMethod.getReturnType();
      }
      else if (writeMethod != null) {
        propertyType = writeMethod.getParameterTypes()[0];
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
      String name = getName();
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

  /**
   * original property name
   */
  @Override
  public String getName() {
    return getPropertyName();
  }

  @Override
  public int getModifiers() {
    if (readMethod != null) {
      return readMethod.getModifiers();
    }
    else if (writeMethod != null) {
      return writeMethod.getModifiers();
    }
    return Modifier.PRIVATE;
  }

  @Override
  public boolean isSynthetic() {
    if (readMethod != null) {
      return readMethod.isSynthetic();
    }
    else if (writeMethod != null) {
      return writeMethod.isSynthetic();
    }
    return true;
  }

  /**
   * read only
   *
   * @since 3.0.2
   */
  public boolean isReadOnly() {
    return writeMethod == null;
  }

  /**
   * can write
   *
   * @since 4.0
   */
  public boolean isWriteable() {
    return writeMethod != null;
  }

  /**
   * can read
   *
   * @since 4.0
   */
  public boolean isReadable() {
    return readMethod != null;
  }

  /**
   * @since 4.0
   */
  @Deprecated
  public String getPropertyName() {
    return name;
  }

  /**
   * Returns the {@code Class} object representing the class or interface
   * that declares the field represented by this {@code Field} object.
   *
   * @since 4.0
   */
  @Override
  public Class<?> getDeclaringClass() {
    if (declaringClass == null) {
      if (readMethod != null) {
        declaringClass = readMethod.getDeclaringClass();
      }
      else if (writeMethod != null) {
        declaringClass = writeMethod.getDeclaringClass();
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

  @Nullable
  public MethodParameter getWriteMethodParameter() {
    MethodParameter writeMethodParameter = this.writeMethodParameter;
    if (writeMethodParameter == null && getWriteMethod() != null) {
      writeMethodParameter = new MethodParameter(getWriteMethod(), 0).withContainingClass(getDeclaringClass());
      this.writeMethodParameter = writeMethodParameter;
    }
    return writeMethodParameter;
  }

  /**
   * If method based bean-property
   */
  @Nullable
  public MethodParameter getMethodParameter() {
    MethodParameter methodParameter = this.methodParameter;
    if (methodParameter == null) {
      methodParameter = resolveMethodParameter();
      this.methodParameter = methodParameter;
    }
    return methodParameter;
  }

  private MethodParameter resolveMethodParameter() {
    MethodParameter read = resolveReadMethodParameter();
    MethodParameter write = getWriteMethodParameter();
    if (write == null) {
      if (read == null) {
        throw new IllegalStateException("Property '" + name + "' in '" + declaringClass + "' is neither readable nor writeable");
      }
      return read;
    }
    if (read != null) {
      Class<?> readType = read.getParameterType();
      Class<?> writeType = write.getParameterType();
      if (!writeType.equals(readType) && writeType.isAssignableFrom(readType)) {
        return read;
      }
    }
    return write;
  }

  @Nullable
  private MethodParameter resolveReadMethodParameter() {
    if (getReadMethod() == null) {
      return null;
    }
    return new MethodParameter(getReadMethod(), -1).withContainingClass(getDeclaringClass());
  }

  // AnnotatedElement

  @Override
  public boolean isAnnotationPresent(@NonNull Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : getAnnotations()) {
      if (annotation.annotationType() == annotationClass) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T getAnnotation(@NonNull Class<T> annotationClass) {
    for (Annotation annotation : getAnnotations()) {
      if (annotation.annotationType() == annotationClass) {
        return (T) annotation;
      }
    }
    return null;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getAnnotations();
  }

  @Override
  public Annotation[] getAnnotations() {
    if (this.annotations == null) {
      this.annotations = resolveAnnotations();
    }
    return this.annotations;
  }

  private Annotation[] resolveAnnotations() {
    Annotation[] annotations = annotationCache.get(this);
    if (annotations == null) {
      Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
      addAnnotationsToMap(annotationMap, getReadMethod());
      addAnnotationsToMap(annotationMap, getWriteMethod());
      addAnnotationsToMap(annotationMap, getField());
      annotations = annotationMap.values().toArray(Constant.EMPTY_ANNOTATION_ARRAY);
      annotationCache.put(this, annotations);
    }
    return annotations;
  }

  private void addAnnotationsToMap(
          Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {
    if (object != null) {
      for (Annotation annotation : object.getAnnotations()) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
    }
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof BeanProperty property) {
      return Objects.equals(name, property.name)
              && Objects.equals(readMethod, property.readMethod)
              && Objects.equals(writeMethod, property.writeMethod)
              && Objects.equals(propertyType, property.propertyType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), field, name, readMethod, writeMethod);
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
  public static BeanProperty valueOf(
          @Nullable Method readMethod, @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
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
  public static BeanProperty valueOf(
          String propertyName, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalStateException("Property is neither readable nor writeable");
    }
    return new BeanProperty(propertyName, readMethod, writeMethod, declaringClass);
  }

}
