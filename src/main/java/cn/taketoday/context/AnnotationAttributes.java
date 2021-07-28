/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.context.support.AnnotationValueCapable;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.OrderUtils;

import static java.lang.String.format;

/**
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2018-12-14 13:45
 * @since 2.1.1
 */
public class AnnotationAttributes
        extends LinkedHashMap<String, Object> implements Ordered {

  private static final long serialVersionUID = 1L;

  private static final String UNKNOWN = "unknown";

  private final String displayName;
  private final Class<? extends Annotation> annotationType;

  public AnnotationAttributes() {
    this.annotationType = null;
    this.displayName = UNKNOWN;
  }

  public AnnotationAttributes(int initialCapacity) {
    super(initialCapacity, 0.75f);
    this.annotationType = null;
    this.displayName = UNKNOWN;
  }

  public AnnotationAttributes(Class<? extends Annotation> annotationType, int initialCapacity) {
    super(initialCapacity, 0.75f);
    Assert.notNull(annotationType, "'annotationType' must not be null");
    this.annotationType = annotationType;
    this.displayName = annotationType.getName();
  }

  public AnnotationAttributes(Map<String, Object> map) {
    super(map);
    this.annotationType = null;
    this.displayName = UNKNOWN;
  }

  public AnnotationAttributes(AnnotationAttributes other) {
    super(other);
    this.annotationType = other.annotationType;
    this.displayName = other.displayName;
  }

  public AnnotationAttributes(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "'annotationType' must not be null");

    this.annotationType = annotationType;
    this.displayName = annotationType.getName();
  }

  public Class<? extends Annotation> annotationType() {
    return this.annotationType;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void add(String name, Object attribute) {
    final Object exist = get(name);
    if (exist != null) {
      final ArrayList values;
      if (exist instanceof List) {
        // more than two values
        values = (ArrayList) exist;
      }
      else {
        values = new ArrayList<>();
        values.add(exist);
      }

      values.add(attribute);
      attribute = values;
    }
    put(name, attribute);
  }

  //
  // ---------------------------------------

  public String getString(String attributeName) {
    return getAttribute(attributeName, String.class);
  }

  public String[] getStringArray(String attributeName) {
    return getAttribute(attributeName, String[].class);
  }

  @SuppressWarnings("unchecked")
  public <T> Class<? extends T>[] getClassArray(String attributeName) {
    return getAttribute(attributeName, Class[].class);
  }

  @SuppressWarnings("unchecked")
  public <T> Class<? extends T> getClass(String attributeName) {
    return getAttribute(attributeName, Class.class);
  }

  public boolean getBoolean(String attributeName) {
    return getAttribute(attributeName, Boolean.class);
  }

  @SuppressWarnings("unchecked")
  public <N extends Number> N getNumber(String attributeName) {
    return (N) getAttribute(attributeName, Number.class);
  }

  @SuppressWarnings("unchecked")
  public <E extends Enum<?>> E getEnum(String attributeName) {
    return (E) getAttribute(attributeName, Enum.class);
  }

  @SuppressWarnings("unchecked")
  public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
    return (A[]) getAttribute(attributeName, Array.newInstance(annotationType, 0).getClass());
  }

  /**
   * Get the value of attribute name and cast to target type
   *
   * @param attributeName
   *         The attribute name
   * @param expectedType
   *         target type
   *
   * @return T
   */
  @SuppressWarnings({ "unchecked" })
  public <T> T getAttribute(String attributeName, Class<T> expectedType) {
    Assert.notNull(attributeName, "'attributeName' must not be null or empty");
    Object value = get(attributeName); // get value

    // @since 3.1.0
    if (value instanceof List) {
      // more than two values
      final List<T> list = (List<T>) value;
      if (expectedType.isArray()) {
        // target return type is array
        final Object[] array = (Object[]) Array.newInstance(expectedType.getComponentType(), list.size());
        int i = 0;
        for (final Object target : list) {
          array[i++] = getRealValue(target);
        }
        put(attributeName, array); // replace
        return (T) array;
      }
      else {
        // single value
        final T ret = (T) getRealValue(list.get(0));
        list.set(0, ret);
        return ret;
      }
    }

    // @since 3.1.0
    value = getRealValue(value);

    assertAttributePresence(attributeName, value);
    if (!expectedType.isInstance(value) && expectedType.isArray() && expectedType.getComponentType().isInstance(value)) {
      Object array = Array.newInstance(expectedType.getComponentType(), 1);
      Array.set(array, 0, value);
      value = array;
    }
    assertAttributeType(attributeName, value, expectedType);
    return expectedType.cast(value);
  }

  private Object getRealValue(Object target) {
    if (target instanceof AnnotationValueCapable) {
      target = ((AnnotationValueCapable) target).getAnnotationValue();
    }
    return target;
  }

  private void assertAttributePresence(String attributeName, Object attributeValue) {
    if (attributeValue == null) {
      throw new NullPointerException(
              format("Attribute '%s' not found in attributes for annotation [%s]",
                     attributeName,
                     this.displayName)
      );
    }
  }

  private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
    if (!expectedType.isInstance(attributeValue)) {
      throw new IllegalArgumentException(
              format("Attribute '%s' is of type [%s], but [%s] was expected in attributes for annotation [%s]",
                     attributeName,
                     attributeValue.getClass().getName(),
                     expectedType.getName(),
                     this.displayName)
      );
    }
  }

  @Override
  public String toString() {
    final Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
    final StringBuilder sb = new StringBuilder("{");
    while (entries.hasNext()) {
      Map.Entry<String, Object> entry = entries.next();
      sb.append(entry.getKey());
      sb.append('=');
      sb.append(valueToString(entry.getValue()));
      sb.append(entries.hasNext() ? ", " : "");
    }
    sb.append("}");
    return sb.toString();
  }

  private String valueToString(Object value) {
    if (value == this) {
      return "(this Map)";
    }
    if (value instanceof Object[]) {
      return Arrays.toString((Object[]) value);
    }
    return String.valueOf(value);
  }

  public static AnnotationAttributes fromMap(Map<String, Object> map) {
    if (map == null) {
      return null;
    }
    if (map instanceof AnnotationAttributes) {
      return (AnnotationAttributes) map;
    }
    return new AnnotationAttributes(map);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof AnnotationAttributes) {
      final AnnotationAttributes other = (AnnotationAttributes) object;
      return Objects.equals(annotationType, other.annotationType)
              && Objects.equals(displayName, other.displayName)
              && super.equals(object);
    }
    return false;
  }

  @Override
  public int getOrder() {
    return OrderUtils.getOrder(annotationType);
  }

}
