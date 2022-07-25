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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link InvocationHandler} for an {@link Annotation} that has
 * <em>synthesized</em> (i.e. wrapped in a dynamic proxy) with additional
 * functionality such as attribute alias handling.
 *
 * @param <A> the annotation type
 * @author Sam Brannen
 * @author Phillip Webb
 * @see Annotation
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 * @since 4.0
 */
final class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

  private final Class<A> type;
  private final AttributeMethods attributeMethods;
  private final MergedAnnotation<?> annotation;
  private final ConcurrentHashMap<String, Object> valueCache = new ConcurrentHashMap<>(8);

  @Nullable
  private volatile Integer hashCode;

  @Nullable
  private volatile String string;

  private SynthesizedMergedAnnotationInvocationHandler(MergedAnnotation<A> annotation, Class<A> type) {
    Assert.notNull(type, "Type must not be null");
    Assert.notNull(annotation, "MergedAnnotation must not be null");
    Assert.isTrue(type.isAnnotation(), "Type must be an annotation");
    this.type = type;
    this.annotation = annotation;
    this.attributeMethods = AttributeMethods.forAnnotationType(type);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    if (ReflectionUtils.isEqualsMethod(method)) {
      return annotationEquals(args[0]);
    }
    if (ReflectionUtils.isHashCodeMethod(method)) {
      return annotationHashCode();
    }
    if (ReflectionUtils.isToStringMethod(method)) {
      return annotationToString();
    }
    if (isAnnotationTypeMethod(method)) {
      return this.type;
    }
    if (this.attributeMethods.indexOf(method.getName()) != -1) {
      return getAttributeValue(method);
    }
    throw new AnnotationConfigurationException(String.format(
            "Method [%s] is unsupported for synthesized annotation type [%s]", method, this.type));
  }

  private boolean isAnnotationTypeMethod(Method method) {
    return (method.getName().equals("annotationType") && method.getParameterCount() == 0);
  }

  /**
   * See {@link Annotation#equals(Object)} for a definition of the required algorithm.
   *
   * @param other the other object to compare against
   */
  private boolean annotationEquals(Object other) {
    if (this == other) {
      return true;
    }
    if (!this.type.isInstance(other)) {
      return false;
    }
    for (Method attribute : attributeMethods.attributes) {
      Object thisValue = getAttributeValue(attribute);
      Object otherValue = ReflectionUtils.invokeMethod(attribute, other);
      if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
        return false;
      }
    }
    return true;
  }

  /**
   * See {@link Annotation#hashCode()} for a definition of the required algorithm.
   */
  private int annotationHashCode() {
    Integer hashCode = this.hashCode;
    if (hashCode == null) {
      hashCode = computeHashCode();
      this.hashCode = hashCode;
    }
    return hashCode;
  }

  private Integer computeHashCode() {
    int hashCode = 0;
    for (Method attribute : attributeMethods.attributes) {
      Object value = getAttributeValue(attribute);
      hashCode += (127 * attribute.getName().hashCode()) ^ getValueHashCode(value);
    }
    return hashCode;
  }

  private int getValueHashCode(Object value) {
    // Use Arrays.hashCode(...) since ObjectUtils doesn't comply
    // with the requirements specified in Annotation#hashCode().
    if (value instanceof boolean[]) {
      return Arrays.hashCode((boolean[]) value);
    }
    if (value instanceof byte[]) {
      return Arrays.hashCode((byte[]) value);
    }
    if (value instanceof char[]) {
      return Arrays.hashCode((char[]) value);
    }
    if (value instanceof double[]) {
      return Arrays.hashCode((double[]) value);
    }
    if (value instanceof float[]) {
      return Arrays.hashCode((float[]) value);
    }
    if (value instanceof int[]) {
      return Arrays.hashCode((int[]) value);
    }
    if (value instanceof long[]) {
      return Arrays.hashCode((long[]) value);
    }
    if (value instanceof short[]) {
      return Arrays.hashCode((short[]) value);
    }
    if (value instanceof Object[]) {
      return Arrays.hashCode((Object[]) value);
    }
    return value.hashCode();
  }

  private String annotationToString() {
    String string = this.string;
    if (string == null) {
      StringBuilder builder = new StringBuilder("@").append(this.type.getName()).append('(');
      Method[] attributes = this.attributeMethods.attributes;
      for (int i = 0; i < attributes.length; i++) {
        Method attribute = attributes[i];
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(attribute.getName());
        builder.append('=');
        builder.append(toString(getAttributeValue(attribute)));
      }
      builder.append(')');
      string = builder.toString();
      this.string = string;
    }
    return string;
  }

  private String toString(Object value) {
    if (value instanceof Class) {
      return ((Class<?>) value).getName();
    }
    if (value.getClass().isArray()) {
      StringBuilder builder = new StringBuilder("[");
      int length = Array.getLength(value);
      for (int i = 0; i < length; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(toString(Array.get(value, i)));
      }
      builder.append(']');
      return builder.toString();
    }
    return String.valueOf(value);
  }

  private Object getAttributeValue(Method method) {
    Object value = this.valueCache.computeIfAbsent(method.getName(), attributeName -> {
      Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(method.getReturnType());
      return this.annotation.getValue(attributeName, type)
              .orElseThrow(() -> new NoSuchElementException(
                      "No value found for attribute named '" + attributeName +
                              "' in merged annotation " + this.annotation.getType().getName()));
    });

    // Clone non-empty arrays so that users cannot alter the contents of values in our cache.
    if (value.getClass().isArray() && Array.getLength(value) > 0) {
      value = cloneArray(value);
    }

    return value;
  }

  /**
   * Clone the provided array, ensuring that the original component type is retained.
   *
   * @param array the array to clone
   */
  private Object cloneArray(Object array) {
    if (array instanceof boolean[]) {
      return ((boolean[]) array).clone();
    }
    if (array instanceof byte[]) {
      return ((byte[]) array).clone();
    }
    if (array instanceof char[]) {
      return ((char[]) array).clone();
    }
    if (array instanceof double[]) {
      return ((double[]) array).clone();
    }
    if (array instanceof float[]) {
      return ((float[]) array).clone();
    }
    if (array instanceof int[]) {
      return ((int[]) array).clone();
    }
    if (array instanceof long[]) {
      return ((long[]) array).clone();
    }
    if (array instanceof short[]) {
      return ((short[]) array).clone();
    }

    // else
    return ((Object[]) array).clone();
  }

  @SuppressWarnings("unchecked")
  static <A extends Annotation> A createProxy(MergedAnnotation<A> annotation, Class<A> type) {
    ClassLoader classLoader = type.getClassLoader();
    InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(annotation, type);
    Class<?>[] interfaces = isVisible(classLoader, SynthesizedAnnotation.class) ?
                            new Class<?>[] { type, SynthesizedAnnotation.class } : new Class<?>[] { type };
    return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
  }

  private static boolean isVisible(ClassLoader classLoader, Class<?> interfaceClass) {
    if (classLoader == interfaceClass.getClassLoader()) {
      return true;
    }
    try {
      return Class.forName(interfaceClass.getName(), false, classLoader) == interfaceClass;
    }
    catch (ClassNotFoundException ex) {
      return false;
    }
  }

}
