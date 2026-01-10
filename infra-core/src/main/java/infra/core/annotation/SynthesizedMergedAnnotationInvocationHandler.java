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

package infra.core.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.NoSuchElementException;

import infra.util.ClassUtils;
import infra.util.ObjectUtils;

import static infra.core.annotation.AttributeMethods.getName;

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
        Object otherValue = AnnotationUtils.invokeAnnotationMethod(attribute, other);
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
    Class<?> type = value.getClass();
    if (type.isArray()) {
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
    if (type == String.class) {
      return '\'' + ((String) value) + '\'';
    }
    if (type == Character.class) {
      return '\'' + value.toString() + '\'';
    }
    if (type == Byte.class) {
      return String.format("(byte) 0x%02X", value);
    }
    if (type == Long.class) {
      return Long.toString((Long) value) + 'L';
    }
    if (type == Float.class) {
      return Float.toString((Float) value) + 'f';
    }
    if (type == Double.class) {
      return Double.toString((Double) value) + 'd';
    }
    if (value instanceof Enum<?> e) {
      return e.name();
    }
    if (type == Class.class) {
      return getName((Class<?>) value) + ".class";
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
            throw new NoSuchElementException("No value found for attribute named '%s' in merged annotation %s"
                    .formatted(method.getName(), getName(this.annotation.getType())));
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
  static Object cloneArray(Object array) {
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

}
