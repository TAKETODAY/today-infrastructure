/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.reflect;

import org.jspecify.annotations.Nullable;

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
import infra.core.Nullness;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;
import infra.lang.Constant;
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

  /** @since 3.0.4 */
  private transient @Nullable TypeDescriptor typeDescriptor;

  /** @since 5.0 */
  private final boolean writeableField;

  /** @since 4.0 */
  private @Nullable Class<?> propertyType;

  /** @since 4.0 */
  private @Nullable Class<?> declaringClass;

  /** @since 4.0 */
  private boolean fieldIsNull;

  private transient Annotation @Nullable [] annotations;

  private transient @Nullable MergedAnnotations mergedAnnotations;

  // @since 4.0
  private transient @Nullable ResolvableType resolvableType;

  // @since 4.0
  private transient @Nullable MethodParameter methodParameter;

  // @since 4.0
  protected transient @Nullable MethodParameter writeMethodParameter;

  protected transient @Nullable Field field;

  protected final String name;

  /** @since 4.0 */
  protected final @Nullable Method readMethod;

  /** @since 4.0 */
  protected final @Nullable Method writeMethod;

  /**
   * @since 5.0
   */
  public Property(Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    this.name = field.getName();
    this.field = field;
    this.propertyType = field.getType();
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = field.getDeclaringClass();
    this.writeableField = !Modifier.isFinal(field.getModifiers());
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
    this.writeableField = false;
  }

  /**
   * Returns the {@link TypeDescriptor} for this property.
   * <p>This method caches the result to avoid repeated construction.
   *
   * @return the type descriptor for this property
   * @since 3.0.4
   */
  public final TypeDescriptor getTypeDescriptor() {
    TypeDescriptor typeDescriptor = this.typeDescriptor;
    if (typeDescriptor == null) {
      typeDescriptor = new TypeDescriptor(this);
      this.typeDescriptor = typeDescriptor;
    }
    return typeDescriptor;
  }

  /**
   * Returns the {@link ResolvableType} for this property.
   * <p>This method caches the result to avoid repeated resolution.
   *
   * @return the resolvable type for this property
   * @since 4.0
   */
  public final ResolvableType getResolvableType() {
    ResolvableType resolvableType = this.resolvableType;
    if (resolvableType == null) {
      resolvableType = createResolvableType();
      this.resolvableType = resolvableType;
    }
    return resolvableType;
  }

  protected ResolvableType createResolvableType() {
    if (isMethodBased()) {
      return ResolvableType.forMethodParameter(getMethodParameter());
    }

    Field field = getField();
    if (field != null) {
      return ResolvableType.forField(field);
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
      else if (getField() != null) {
        propertyType = getField().getType();
      }
      else {
        throw new IllegalStateException("should never get here");
      }
    }
    return propertyType;
  }

  /**
   * Retrieves the underlying {@link Field} for this property.
   * <p>If the field is not directly available (e.g., in method-based properties),
   * this method attempts to locate it by name within the declaring class,
   * trying the original name, uncapitalized name, and capitalized name.
   *
   * @return the corresponding {@code Field}, or {@code null} if no matching field is found
   * (which typically indicates a synthetic property or one based solely on methods)
   */
  public @Nullable Field getField() {
    if (field == null && !fieldIsNull) {
      String name = getName();
      if (StringUtils.isEmpty(name)) {
        return null;
      }
      Class<?> declaringClass = getDeclaringClass();
      field = ReflectionUtils.findField(declaringClass, name);
      if (field == null) {
        field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
        if (field == null) {
          field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
        }
      }
      fieldIsNull = field == null;
    }
    return field;
  }

  /**
   * Returns the name of this property.
   *
   * @return the property name
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
    else if (getField() != null) {
      return getField().getModifiers();
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
    else if (getField() != null) {
      return getField().isSynthetic();
    }
    return true;
  }

  /**
   * Determine whether this property is writable.
   * <p>A property is considered writable if it has a non-final field or a write method (setter).
   *
   * @return {@code true} if the property can be written to, {@code false} otherwise
   * @since 4.0
   */
  public boolean isWriteable() {
    return writeableField || writeMethod != null;
  }

  /**
   * Determine whether this property is readable.
   * <p>A property is considered readable if it has a read method (getter) or an accessible field.
   *
   * @return {@code true} if the property can be read from, {@code false} otherwise
   * @since 4.0
   */
  public boolean isReadable() {
    return readMethod != null || getField() != null;
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
    Field field = getField();
    if (field != null) {
      return Nullness.forField(field) == Nullness.NULLABLE;
    }
    if (readMethod != null) {
      return Nullness.forMethodReturnType(readMethod) == Nullness.NULLABLE;
    }
    return isMethodBased() && Nullness.forMethodParameter(getMethodParameter()) == Nullness.NULLABLE;
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
      else if (getField() != null) {
        declaringClass = getField().getDeclaringClass();
      }
    }
    return declaringClass;
  }

  /**
   * Returns the read method for this property, if any.
   *
   * @return the read method, or {@code null} if none
   */
  public @Nullable Method getReadMethod() {
    return readMethod;
  }

  /**
   * Returns the write method for this property, if any.
   *
   * @return the write method, or {@code null} if none
   */
  public @Nullable Method getWriteMethod() {
    return writeMethod;
  }

  /**
   * Determine whether this property is based on a getter/setter method pair
   * rather than a direct field access.
   *
   * @return {@code true} if the property is accessed via methods, {@code false} otherwise
   * @since 5.0
   */
  public boolean isMethodBased() {
    return readMethod != null || writeMethod != null;
  }

  /**
   * Returns the {@link MethodParameter} for the write method of this property.
   * <p>This is typically used for validation or conversion purposes on the setter parameter.
   *
   * @return the write method parameter, or {@code null} if no write method exists
   */
  public @Nullable MethodParameter getWriteMethodParameter() {
    MethodParameter writeMethodParameter = this.writeMethodParameter;
    if (writeMethodParameter == null && writeMethod != null) {
      writeMethodParameter = new MethodParameter(writeMethod, 0)
              .withContainingClass(getDeclaringClass());
      this.writeMethodParameter = writeMethodParameter;
    }
    return writeMethodParameter;
  }

  /**
   * Returns the {@link MethodParameter} for this property.
   * <p>If the property is method-based (i.e., accessed via getter/setter), this returns
   * the parameter descriptor for the read or write method, preferring the read method
   * if both are available and compatible. If the property is field-based, this method
   * will still return a valid {@code MethodParameter} derived from the underlying field's
   * context if possible, or throw an exception if no method or field information is available.
   * <p>The result is cached to avoid repeated resolution.
   *
   * @return the method parameter descriptor for this property
   * @throws IllegalStateException if the property is neither readable nor writeable
   * @since 4.0
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

  private @Nullable MethodParameter resolveReadMethodParameter() {
    if (getReadMethod() == null) {
      return null;
    }
    return new MethodParameter(getReadMethod(), -1).withContainingClass(getDeclaringClass());
  }

  // AnnotatedElement

  /**
   * Returns the {@link MergedAnnotations} for this property.
   * <p>This method merges annotations from the read method, write method, and underlying field,
   * prioritizing interface methods where applicable. The result is cached to avoid repeated resolution.
   *
   * @return the merged annotations for this property
   * @since 5.0
   */
  public MergedAnnotations mergedAnnotations() {
    MergedAnnotations annotations = this.mergedAnnotations;
    if (annotations == null) {
      annotations = MergedAnnotations.from(this, getAnnotations());
      this.mergedAnnotations = annotations;
    }
    return annotations;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : getAnnotations(false)) {
      if (annotation.annotationType() == annotationClass) {
        return true;
      }
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Annotation> @Nullable T getAnnotation(Class<T> annotationClass) {
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
  public boolean equals(@Nullable Object o) {
    if (this == o)
      return true;
    if (o instanceof Property other) {
      return Objects.equals(name, other.name)
              && Objects.equals(field, other.field)
              && Objects.equals(readMethod, other.readMethod)
              && Objects.equals(writeMethod, other.writeMethod)
              && Objects.equals(propertyType, other.propertyType);
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
