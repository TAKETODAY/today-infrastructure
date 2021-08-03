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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import cn.taketoday.context.support.AnnotationValue;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.OrderUtils;

import static java.lang.String.format;

/**
 * single or multi - value map
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2018-12-14 13:45
 * @since 2.1.1
 */
@SuppressWarnings("rawtypes")
public class AnnotationAttributes
        /*extends LinkedHashMap<String, Object>*/ implements Ordered, Serializable {
  private static final long serialVersionUID = 1L;

  private static final String UNKNOWN = "unknown";

  /** key - value */
  private final ArrayList<Object> values;//= new ArrayList<>();

  private String displayName;
  private Class annotationType;

  private int size = 0;

  public AnnotationAttributes() {
    this(new ArrayList<>());
    this.annotationType = null;
    this.displayName = UNKNOWN;
  }

  public AnnotationAttributes(int initialCapacity) {
    this(new ArrayList<>(initialCapacity));
    this.annotationType = null;
    this.displayName = UNKNOWN;
  }

  public AnnotationAttributes(Class annotationType) {
    this(annotationType, 16);
  }

  public AnnotationAttributes(Class annotationType, int initialCapacity) {
    this(new ArrayList<>(initialCapacity));
    Assert.notNull(annotationType, "'annotationType' must not be null");
    this.annotationType = annotationType;
    this.displayName = annotationType.getName();
  }

  public AnnotationAttributes(Map<String, Object> map) {
    this(new ArrayList<>(map.size() * 2));
    this.annotationType = null;
    this.displayName = UNKNOWN;
    putAll(map);
  }

  AnnotationAttributes(ArrayList<Object> values) {
    this.values = values;
  }

  public AnnotationAttributes(AnnotationAttributes other) {
    this.values = new ArrayList<>(other.values);
    this.annotationType = other.annotationType;
    this.displayName = other.displayName;
    this.size = other.size;
  }

  @SuppressWarnings("unchecked")
  public Class<? extends Annotation> annotationType() {
    return this.annotationType;
  }

  // Get
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

    // @since 4.0
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

    // @since 4.0
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
    if (target instanceof AnnotationValue) {
      target = ((AnnotationValue) target).get();
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

  public Object get(String name) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    // key - value
    for (int i = 0; i < size; i += 2) {
      Object key = values.get(i);
      if (Objects.equals(key, name)) {
        return values.get(i + 1);
      }
    }
    return null;
  }

  // put

  //  @Override
  public void put(String key, Object value) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    // key - value
    for (int i = 0; i < size; i += 2) {
      if (Objects.equals(values.get(i), key)) {
        // replace
        values.set(i + 1, value);
        return;
      }
    }
    this.size++;
    values.add(key);
    values.add(value);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void add(String name, Object attribute) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    // key - value
    for (int i = 0; i < size; i += 2) {
      if (Objects.equals(values.get(i), name)) {
        List list;
        Object exist = values.get(i + 1);
        if (exist instanceof List) {
          // more than two values
          list = (List) exist;
        }
        else {
          list = new ArrayList<>();
          list.add(exist);
        }
        list.add(attribute);
        values.set(i + 1, list);
        return;
      }
    }
    this.size++;
    values.add(name);
    values.add(attribute);
  }

  public void putAll(Map<String, Object> attributes) {
    ArrayList<Object> thisValues = this.values;
    for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
      thisValues.add(entry.getKey());
      thisValues.add(entry.getValue());
    }
  }

  public void putAll(AnnotationAttributes attributes) {
    ArrayList<Object> thisValues = this.values;
    ArrayList<Object> otherValues = new ArrayList<>(attributes.values);
    int thisSize = thisValues.size();
    if (thisSize != 0) {
      for (int i = 0; i < thisSize; i++) {
        Object thisName = thisValues.get(i++);
        int otherSize = otherValues.size();
        for (int j = 0; j < otherSize; j++) {
          Object name = otherValues.get(j++);
          if (Objects.equals(name, thisName)) {
            Object otherValue = otherValues.get(j);
            // just override value
            thisValues.set(i, otherValue);
            // mark
            otherValues.remove(j); // j is current otherValue's value index
            otherValues.remove(j - 1); // remove key
            break;
          }
        }
      }
      // match complete
      if (otherValues.isEmpty()) {
        return;
      }
    }
    this.size += otherValues.size() / 2;
    thisValues.addAll(otherValues);
  }

  public void addAll(Map<String, Object> attributes) {
    ArrayList<Object> thisValues = this.values;
    for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
      thisValues.add(entry.getKey());
      thisValues.add(entry.getValue());
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void addAll(AnnotationAttributes attributes) {
    ArrayList<Object> thisValues = this.values;
    ArrayList<Object> otherValues = attributes.values;
    int thisSize = thisValues.size();
    if (thisSize != 0) {
      for (int i = 0; i < thisSize; i++) {
        Object thisName = thisValues.get(i++);
        int otherSize = otherValues.size();
        for (int j = 0; j < otherSize; j++) {
          Object name = otherValues.get(j++);
          if (Objects.equals(name, thisName)) {
            Object otherValue = otherValues.get(j);
            Object thisValue = thisValues.get(i);
            if (thisValue instanceof List) {
              if (otherValue instanceof List) {
                ((List) thisValue).addAll((List) otherValue);
              }
              else {
                ((List) thisValue).add(otherValue);
              }
            }
            else {
              ArrayList<Object> list = new ArrayList<>();
              if (otherValue instanceof List) {
                list.addAll(((List) otherValue));
              }
              else {
                list.add(otherValue);
              }
              list.add(thisValue);
              thisValues.set(i, list);
            }
            // mark
            otherValues.remove(j); // j is current otherValue's value index
            otherValues.remove(j - 1); // remove key
            break;
          }
        }
      }
      // match complete
      if (otherValues.isEmpty()) {
        return;
      }
    }
    this.size += otherValues.size() / 2;
    thisValues.addAll(otherValues);
  }

  /**
   * remove an attribute by given name
   *
   * @param name
   *         attribute-name
   */
  public void remove(String name) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i += 2) {
      Object key = values.get(i);
      if (Objects.equals(key, name)) {
        values.remove(i);
        values.remove(i + 1);
        this.size--;
        return;
      }
    }
  }

  public void forEach(BiConsumer<String, Object> consumer) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i++) {
      consumer.accept((String) values.get(i++), values.get(i));
    }
  }

  public Iterable<Map.Entry<String, Object>> entrySet() {
    return toMap().entrySet();
  }

  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<>();
    copyToMap(map);
    return map;
  }

  public void copyToMap(Map<String, Object> map) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i++) {
      map.put((String) values.get(i++), values.get(i));
    }
  }

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i++) {
      sb.append(values.get(i++));
      sb.append('=');
      sb.append(valueToString(values.get(i)));
      if (i + 1 != size) {
        sb.append(", ");
      }
    }
    sb.append('}');
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
