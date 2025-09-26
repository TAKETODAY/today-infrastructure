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

package infra.core.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.ReflectionUtils;

/**
 * Provides a quick way to access the attribute methods of an {@link Annotation}
 * with consistent ordering as well as a few useful utility methods.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class AttributeMethods {
  static final AttributeMethods NONE = new AttributeMethods(null, Constant.EMPTY_METHODS);

  static final ConcurrentReferenceHashMap<Class<? extends Annotation>, AttributeMethods>
          cache = new ConcurrentReferenceHashMap<>();

  private static final Comparator<Method> methodComparator = (m1, m2) -> {
    if (m1 != null && m2 != null) {
      return m1.getName().compareTo(m2.getName());
    }
    return m1 != null ? -1 : 1;
  };

  @Nullable
  private final Class<? extends Annotation> annotationType;

  public final Method[] attributes;

  private final boolean[] canThrowTypeNotPresentException;

  /**
   * Determine if at least one of the attribute methods has a default value.
   *
   * <p>{@code true} if there is at least one attribute method with a default value
   */
  public final boolean hasDefaultValueMethod;

  /**
   * Determine if at least one of the attribute methods is a nested annotation.
   *
   * <p>{@code true} if there is at least one attribute method with a nested
   * annotation type
   */
  public final boolean hasNestedAnnotation;

  private AttributeMethods(@Nullable Class<? extends Annotation> annotationType, Method[] attributes) {
    this.annotationType = annotationType;
    this.attributes = attributes;
    this.canThrowTypeNotPresentException = new boolean[attributes.length];
    boolean foundDefaultValueMethod = false;
    boolean foundNestedAnnotation = false;
    for (int i = 0; i < attributes.length; i++) {
      Method method = attributes[i];
      Class<?> type = method.getReturnType();
      if (!foundDefaultValueMethod && (method.getDefaultValue() != null)) {
        foundDefaultValueMethod = true;
      }
      if (!foundNestedAnnotation && (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation()))) {
        foundNestedAnnotation = true;
      }
      ReflectionUtils.makeAccessible(method);
      this.canThrowTypeNotPresentException[i] = (type == Class.class || type == Class[].class || type.isEnum());
    }
    this.hasDefaultValueMethod = foundDefaultValueMethod;
    this.hasNestedAnnotation = foundNestedAnnotation;
  }

  /**
   * Determine if values from the given annotation can be safely accessed without
   * causing any {@link TypeNotPresentException TypeNotPresentExceptions}.
   * <p>This method is designed to cover Google App Engine's late arrival of such
   * exceptions for {@code Class} values (instead of the more typical early
   * {@code Class.getAnnotations() failure} on a regular JVM).
   *
   * @param annotation the annotation to check
   * @return {@code true} if all values are present
   * @see #validate(Annotation)
   */
  boolean isValid(Annotation annotation) {
    assertAnnotation(annotation);
    Method[] attributes = this.attributes;
    for (int i = 0; i < attributes.length; i++) {
      if (canThrowTypeNotPresentException(i)) {
        try {
          AnnotationUtils.invokeAnnotationMethod(attributes[i], annotation);
        }
        catch (IllegalStateException ex) {
          throw ex;
        }
        catch (Throwable ex) {
          // TypeNotPresentException etc. -> annotation type not actually loadable.
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Check if values from the given annotation can be safely accessed without causing
   * any {@link TypeNotPresentException TypeNotPresentExceptions}. In particular,
   * this method is designed to cover Google App Engine's late arrival of such
   * exceptions for {@code Class} values (instead of the more typical early
   * {@code Class.getAnnotations() failure}.
   *
   * @param annotation the annotation to validate
   * @throws IllegalStateException if a declared {@code Class} attribute could not be read
   * @see #isValid(Annotation)
   */
  void validate(Annotation annotation) {
    assertAnnotation(annotation);
    Method[] attributes = this.attributes;
    for (int i = 0; i < attributes.length; i++) {
      if (canThrowTypeNotPresentException(i)) {
        try {
          AnnotationUtils.invokeAnnotationMethod(attributes[i], annotation);
        }
        catch (IllegalStateException ex) {
          throw ex;
        }
        catch (Throwable ex) {
          throw new IllegalStateException("Could not obtain annotation attribute value for %s declared on @%s"
                  .formatted(attributes[i].getName(), getName(annotation.annotationType())), ex);
        }
      }
    }
  }

  private void assertAnnotation(Annotation annotation) {
    Assert.notNull(annotation, "Annotation is required");
    if (this.annotationType != null) {
      Assert.isInstanceOf(this.annotationType, annotation);
    }
  }

  /**
   * Get the attribute with the specified name or {@code null} if no
   * matching attribute exists.
   *
   * @param name the attribute name to find
   * @return the attribute method or {@code null}
   */
  @Nullable
  Method get(String name) {
    int index = indexOf(name);
    return index != -1 ? attributes[index] : null;
  }

  /**
   * Get the attribute at the specified index.
   *
   * @param index the index of the attribute to return
   * @return the attribute method
   * @throws IndexOutOfBoundsException if the index is out of range
   * (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  Method get(int index) {
    return attributes[index];
  }

  /**
   * Determine if the attribute at the specified index could throw a
   * {@link TypeNotPresentException} when accessed.
   *
   * @param index the index of the attribute to check
   * @return {@code true} if the attribute can throw a
   * {@link TypeNotPresentException}
   */
  boolean canThrowTypeNotPresentException(int index) {
    return canThrowTypeNotPresentException[index];
  }

  /**
   * Get the index of the attribute with the specified name, or {@code -1}
   * if there is no attribute with the name.
   *
   * @param name the name to find
   * @return the index of the attribute, or {@code -1}
   */
  int indexOf(String name) {
    Method[] attributes = this.attributes;
    for (int i = 0; i < attributes.length; i++) {
      if (attributes[i].getName().equals(name)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Get the index of the specified attribute, or {@code -1} if the
   * attribute is not in this collection.
   *
   * @param attribute the attribute to find
   * @return the index of the attribute, or {@code -1}
   */
  int indexOf(Method attribute) {
    Method[] attributes = this.attributes;
    for (int i = 0; i < attributes.length; i++) {
      if (attributes[i].equals(attribute)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Get the number of attributes in this collection.
   *
   * @return the number of attributes
   */
  int size() {
    return attributes.length;
  }

  /**
   * Get the attribute methods for the given annotation type.
   *
   * @param annotationType the annotation type
   * @return the attribute methods for the annotation type
   */
  static AttributeMethods forAnnotationType(@Nullable Class<? extends Annotation> annotationType) {
    if (annotationType == null) {
      return NONE;
    }
    return cache.computeIfAbsent(annotationType, AttributeMethods::compute);
  }

  private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
    Method[] methods = annotationType.getDeclaredMethods();
    int size = methods.length;
    for (int i = 0; i < methods.length; i++) {
      if (!isAttributeMethod(methods[i])) {
        methods[i] = null;
        size--;
      }
    }
    if (size == 0) {
      return NONE;
    }
    Arrays.sort(methods, methodComparator);
    Method[] attributeMethods = Arrays.copyOf(methods, size);
    return new AttributeMethods(annotationType, attributeMethods);
  }

  private static boolean isAttributeMethod(Method method) {
    return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
  }

  /**
   * Create a description for the given attribute method suitable to use in
   * exception messages and logs.
   *
   * @param attribute the attribute to describe
   * @return a description of the attribute
   */
  static String describe(@Nullable Method attribute) {
    if (attribute == null) {
      return "(none)";
    }
    return describe(attribute.getDeclaringClass(), attribute.getName());
  }

  /**
   * Create a description for the given attribute method suitable to use in
   * exception messages and logs.
   *
   * @param annotationType the annotation type
   * @param attributeName the attribute name
   * @return a description of the attribute
   */
  static String describe(@Nullable Class<?> annotationType, @Nullable String attributeName) {
    if (attributeName == null) {
      return "(none)";
    }
    String in = (annotationType != null ? " in annotation [" + annotationType.getName() + "]" : "");
    return "attribute '" + attributeName + "'" + in;
  }

  static String getName(Class<?> clazz) {
    String canonicalName = clazz.getCanonicalName();
    return (canonicalName != null ? canonicalName : clazz.getName());
  }

}
