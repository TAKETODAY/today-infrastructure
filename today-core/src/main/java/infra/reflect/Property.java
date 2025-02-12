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

package infra.reflect;

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

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.lang.NonNull;
import infra.lang.Nullable;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

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

  @Nullable
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
  @Nullable
  private Class<?> propertyType;

  /** @since 4.0 */
  @Nullable
  private Class<?> declaringClass;

  /** @since 4.0 */
  private boolean fieldIsNull;

  @Nullable
  private transient Annotation[] annotations;

  // @since 4.0
  @Nullable
  private ResolvableType resolvableType;

  // @since 4.0
  @Nullable
  private transient MethodParameter methodParameter;

  // @since 4.0
  @Nullable
  protected transient MethodParameter writeMethodParameter;

  public Property(Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    this.name = field.getName();
    this.field = field;
    this.propertyType = field.getType();
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = field.getDeclaringClass();
  }

  public Property(Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod) {
    this(null, readMethod, writeMethod, objectType);
  }

  public Property(@Nullable String name, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalArgumentException("Property '%s' in '%s' is neither readable nor writeable"
              .formatted(name, declaringClass));
    }
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = declaringClass;
    if (name == null) {
      name = ReflectionUtils.getPropertyName(readMethod, writeMethod);
      Assert.notNull(name, "property name is required");
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

  public final ResolvableType getResolvableType() {
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

  /**
   * Determines if the specified {@code Object} is assignment-compatible
   * with the object represented by this {@code Property}.  This method is
   * the dynamic equivalent of the Java language {@code instanceof}
   * operator. The method returns {@code true} if the specified
   * {@code Object} argument is non-null and can be cast to the
   * reference type represented by this {@code Property} object without
   * raising a {@code ClassCastException.} It returns {@code false}
   * otherwise.
   *
   * <p> Specifically, if this {@code Property} object represents a
   * declared class, this method returns {@code true} if the specified
   * {@code Object} argument is an instance of the represented class (or
   * of any of its subclasses); it returns {@code false} otherwise. If
   * this {@code Property} object represents an array class, this method
   * returns {@code true} if the specified {@code Object} argument
   * can be converted to an object of the array class by an identity
   * conversion or by a widening reference conversion; it returns
   * {@code false} otherwise. If this {@code Property} object
   * represents an interface, this method returns {@code true} if the
   * class or any superclass of the specified {@code Object} argument
   * implements this interface; it returns {@code false} otherwise. If
   * this {@code Property} object represents a primitive type, this method
   * returns {@code false}.
   *
   * @param value the object to check
   * @return true if {@code obj} is an instance of this property-type
   * @see Class#isInstance(Object)
   */
  public boolean isInstance(Object value) {
    return getType().isInstance(value);
  }

  /**
   * Returns a {@code Class} object that identifies the
   * declared type for the field represented by this
   * {@code Field} object.
   *
   * @return a {@code Class} object identifying the declared
   * type of the field represented by this object
   */
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
   * @see Boolean#TYPE
   * @see Character#TYPE
   * @see Byte#TYPE
   * @see Short#TYPE
   * @see Integer#TYPE
   * @see Long#TYPE
   * @see Float#TYPE
   * @see Double#TYPE
   * @see Void#TYPE
   * @see Class#isPrimitive()
   * @since 4.0
   */
  public boolean isPrimitive() {
    return getType().isPrimitive();
  }

  /**
   * Return whether this property which can be {@code null}:
   * either in the form of any variant of a parameter-level {@code Nullable}
   * annotation (such as from JSR-305 or the FindBugs set of annotations),
   * or a language-level nullable type declaration
   *
   * @since 4.0
   */
  public boolean isNullable() {
    for (Annotation ann : getAnnotations(false)) {
      if ("Nullable".equals(ann.annotationType().getSimpleName())) {
        return true;
      }
    }
    return false;
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
        throw new IllegalStateException("Property '%s' in '%s' is neither readable nor writeable"
                .formatted(name, declaringClass));
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
    for (Annotation annotation : getAnnotations(false)) {
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
    for (Annotation annotation : getAnnotations(false)) {
      if (annotation.annotationType() == annotationClass) {
        return (T) annotation;
      }
    }
    return null;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getAnnotations(true);
  }

  @Override
  public Annotation[] getAnnotations() {
    return getAnnotations(true);
  }

  private Annotation[] getAnnotations(boolean clone) {
    Annotation[] annotations = this.annotations;
    if (annotations == null) {
      annotations = resolveAnnotations();
      this.annotations = annotations;
    }
    if (clone && annotations.length > 0) {
      return annotations.clone();
    }
    return annotations;
  }

  private Annotation[] resolveAnnotations() {
    Annotation[] annotations = annotationCache.get(this);
    if (annotations == null) {
      var annotationMap = new LinkedHashMap<Class<? extends Annotation>, Annotation>();
      Method readMethod = getReadMethod();
      Method writeMethod = getWriteMethod();

      if (readMethod != null) {
        Method method = ReflectionUtils.getInterfaceMethodIfPossible(readMethod, getDeclaringClass());
        if (method != readMethod) {
          addAnnotationsToMap(annotationMap, method);
        }
      }

      if (writeMethod != null) {
        Method method = ReflectionUtils.getInterfaceMethodIfPossible(writeMethod, getDeclaringClass());
        if (method != writeMethod) {
          addAnnotationsToMap(annotationMap, method);
        }
      }

      addAnnotationsToMap(annotationMap, readMethod);
      addAnnotationsToMap(annotationMap, writeMethod);
      addAnnotationsToMap(annotationMap, getField());
      annotations = annotationMap.isEmpty() ? Constant.EMPTY_ANNOTATIONS
              : annotationMap.values().toArray(Constant.EMPTY_ANNOTATIONS);
      annotationCache.put(this, annotations);
    }
    return annotations;
  }

  private void addAnnotationsToMap(Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {
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
