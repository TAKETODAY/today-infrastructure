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

package cn.taketoday.reflect;

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A description of a JavaBeans Property that allows us to avoid a dependency on
 * {@code java.beans.PropertyDescriptor}. The {@code java.beans} package
 * is not available in a number of environments (e.g. Android, Java ME), so this is
 * desirable for portability of core conversion facility.
 *
 * <p>Used to build a {@link TypeDescriptor} from a property location. The built
 * {@code TypeDescriptor} can then be used to convert from/to the property type.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/23 01:01
 */
public class Property implements Member, AnnotatedElement, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private static final ConcurrentReferenceHashMap<Property, Annotation[]>
          annotationCache = new ConcurrentReferenceHashMap<>();

  // Nullable
  protected transient Field field;

  /** @since 3.0.4 */
  @Nullable
  private transient TypeDescriptor typeDescriptor;

  protected final String name;

  /** @since 4.0 */
  @Nullable
  protected final Method readMethod;

  /** @since 4.0 */
  @Nullable
  protected final Method writeMethod;

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
  protected transient MethodParameter writeMethodParameter;

  public Property(String name, Field field) {
    this.name = name;
    this.field = field;
    this.propertyType = field.getType();
    this.readMethod = null;
    this.writeMethod = null;
  }

  public Property(Field field) {
    this(field.getName(), field);
  }

  public Property(@Nullable Method readMethod,
          @Nullable Method writeMethod,
          @Nullable Class<?> declaringClass) {
    this(null, readMethod, writeMethod, declaringClass);
  }

  public Property(@Nullable String name,
          @Nullable Method readMethod,
          @Nullable Method writeMethod,
          @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalArgumentException(
              "Property '" + name + "' in '" + declaringClass + "' is neither readable nor writeable");
    }
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = declaringClass;
    if (name == null) {
      name = ReflectionUtils.getPropertyName(readMethod, writeMethod);
    }
    this.name = name;
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

  public boolean isInstance(Object value) {
    return getType().isInstance(value);
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
    return name;
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
    // todo maybe can access field
    return readMethod != null;
  }

  /**
   * is primitive
   *
   * @see java.lang.Boolean#TYPE
   * @see java.lang.Character#TYPE
   * @see java.lang.Byte#TYPE
   * @see java.lang.Short#TYPE
   * @see java.lang.Integer#TYPE
   * @see java.lang.Long#TYPE
   * @see java.lang.Float#TYPE
   * @see java.lang.Double#TYPE
   * @see java.lang.Void#TYPE
   * @see Class#isPrimitive()
   * @since 4.0
   */
  public boolean isPrimitive() {
    return getType().isPrimitive();
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
      annotations = annotationMap.values().toArray(Constant.EMPTY_ANNOTATIONS);
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
    if (o instanceof Property property) {
      return Objects.equals(name, property.name)
              && Objects.equals(readMethod, property.readMethod)
              && Objects.equals(writeMethod, property.writeMethod)
              && Objects.equals(propertyType, property.propertyType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, name, readMethod, writeMethod);
  }

  @Override
  public String toString() {
    return getType().getSimpleName() + " " + getName();
  }

}
