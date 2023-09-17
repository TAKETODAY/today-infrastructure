/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.util.HashMap;
import java.util.NoSuchElementException;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Annotation
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 * @since 4.0
 */
final class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

  private final Class<A> type;
  private final AttributeMethods attributeMethods;
  private final AbstractMergedAnnotation<?> annotation;
  private final HashMap<String, Object> valueCache = new HashMap<>(8);

  @Nullable
  private volatile Integer hash;

  @Nullable
  private volatile String string;

  SynthesizedMergedAnnotationInvocationHandler(AbstractMergedAnnotation<A> annotation, Class<A> type) {
    this.type = type;
    this.annotation = annotation;
    this.attributeMethods = AttributeMethods.forAnnotationType(type);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    String attribute = method.getName();
    int parameterCount = method.getParameterCount();

    // Handle Object and Annotation methods
    if (parameterCount == 1
            && attribute.equals("equals")
            && method.getParameterTypes()[0] == Object.class) {
      return annotationEquals(proxy, args[0]);
    }

    if (parameterCount == 0) {
      return switch (attribute) {
        case "toString" -> annotationToString();
        case "hashCode" -> annotationHashCode();
        case "annotationType" -> type;
        default -> getAttributeValue(method, true);
      };
    }
    throw new AnnotationConfigurationException(String.format(
            "Method [%s] is unsupported for synthesized annotation type [%s]", method, this.type));
  }

  /**
   * See {@link Annotation#equals(Object)} for a definition of the required algorithm.
   *
   * @param other the other object to compare against
   */
  private boolean annotationEquals(Object proxy, Object other) {
    if (proxy != other) {
      if (!type.isInstance(other)) {
        return false;
      }
      for (Method attribute : attributeMethods.attributes) {
        Object thisValue = getAttributeValue(attribute, false);
        Object otherValue = ReflectionUtils.invokeMethod(attribute, other);
        if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * See {@link Annotation#hashCode()} for a definition of the required algorithm.
   */
  private int annotationHashCode() {
    Integer hash = this.hash;
    if (hash == null) {
      synchronized(this) {
        hash = this.hash;
        if (hash == null) {
          hash = computeHashCode();
          this.hash = hash;
        }
      }
    }
    return hash;
  }

  private Integer computeHashCode() {
    int hashCode = 0;
    for (Method attribute : attributeMethods.attributes) {
      Object value = getAttributeValue(attribute, false);
      hashCode += (127 * attribute.getName().hashCode()) ^ ObjectUtils.nullSafeHashCode(value);
    }
    return hashCode;
  }

  private String annotationToString() {
    String string = this.string;
    if (string == null) {
      synchronized(this) {
        string = this.string;
        if (string == null) {
          StringBuilder builder = new StringBuilder("@").append(getName(type)).append('(');
          Method[] attributes = attributeMethods.attributes;
          for (int i = 0; i < attributes.length; i++) {
            Method attribute = attributes[i];
            if (i > 0) {
              builder.append(", ");
            }
            builder.append(attribute.getName());
            builder.append('=');
            builder.append(toString(getAttributeValue(attribute, false)));
          }
          builder.append(')');
          string = builder.toString();
          this.string = string;
        }
      }
    }
    return string;
  }

  /**
   * This method currently does not address the following issues which we may
   * choose to address at a later point in time.
   *
   * <ul>
   * <li>non-ASCII, non-visible, and non-printable characters within a character
   * or String literal are not escaped.</li>
   * <li>formatting for float and double values does not take into account whether
   * a value is not a number (NaN) or infinite.</li>
   * </ul>
   *
   * @param value the attribute value to format
   * @return the formatted string representation
   */
  private String toString(Object value) {
    if (value instanceof Character) {
      return '\'' + value.toString() + '\'';
    }
    if (value instanceof Byte) {
      return String.format("(byte) 0x%02X", value);
    }
    if (value instanceof Long longValue) {
      return Long.toString(longValue) + 'L';
    }
    if (value instanceof Float floatValue) {
      return Float.toString(floatValue) + 'f';
    }
    if (value instanceof Double doubleValue) {
      return Double.toString(doubleValue) + 'd';
    }
    if (value instanceof Enum<?> e) {
      return e.name();
    }
    if (value instanceof Class<?> clazz) {
      return getName(clazz) + ".class";
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

  private Object getAttributeValue(Method method, boolean cloneArray) {
    Object value = valueCache.get(method.getName());
    if (value == null) {
      synchronized(valueCache) {
        value = valueCache.get(method.getName());
        if (value == null) {
          Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(method.getReturnType());
          value = annotation.getAttributeValue(method.getName(), type);
          if (value == null) {
            throw new NoSuchElementException(
                    "No value found for attribute named '" + method.getName() +
                            "' in merged annotation " + annotation.getType().getName());
          }
          valueCache.put(method.getName(), value);
        }
      }
    }

    // Clone non-empty arrays so that users cannot alter the contents of values in our cache.
    if (cloneArray && value.getClass().isArray() && Array.getLength(value) > 0) {
      value = cloneArray(value);
    }
    return value;
  }

  /**
   * Clone the provided array, ensuring that the original component type is retained.
   *
   * @param array the array to clone
   */
  private static Object cloneArray(Object array) {
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
  static <A extends Annotation> A createProxy(AbstractMergedAnnotation<A> annotation, Class<A> type) {
    ClassLoader classLoader = type.getClassLoader();
    InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(annotation, type);
    return (A) Proxy.newProxyInstance(classLoader, new Class<?>[] { type }, handler);
  }

  private static String getName(Class<?> clazz) {
    String canonicalName = clazz.getCanonicalName();
    return (canonicalName != null ? canonicalName : clazz.getName());
  }
}
