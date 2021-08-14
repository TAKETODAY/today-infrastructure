/*
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.core;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import cn.taketoday.asm.AnnotationValueHolder;
import cn.taketoday.core.utils.OrderUtils;

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
        /*extends LinkedHashMap<String, Object>*/ implements Ordered, Serializable, Map<String, Object> {
  private static final long serialVersionUID = 1L;

  private static final String UNKNOWN = "unknown";

  /** key - value */
  private final ArrayList<Object> values;//= new ArrayList<>();

  private Class annotationType;
  private String annotationName = UNKNOWN;

  private int size = 0;

  public AnnotationAttributes() {
    this(new ArrayList<>());
  }

  public AnnotationAttributes(int initialCapacity) {
    this(new ArrayList<>(initialCapacity));
  }

  public AnnotationAttributes(Class annotationType) {
    this(annotationType, 16);
  }

  public AnnotationAttributes(String annotationName) {
    this(16);
    this.annotationName = annotationName;
  }

  public AnnotationAttributes(Class annotationType, int initialCapacity) {
    this(new ArrayList<>(initialCapacity * 2));
    Assert.notNull(annotationType, "'annotationType' must not be null");
    this.annotationType = annotationType;
    this.annotationName = annotationType.getName();
  }

  public AnnotationAttributes(Map<String, Object> map) {
    this(new ArrayList<>(map.size() * 2));
    putAll(map);
  }

  public AnnotationAttributes(Map<String, Object> map, Class annotationType) {
    this(new ArrayList<>(map.size() * 2));
    this.annotationType = annotationType;
    this.annotationName = annotationType.getName();
    putAll(map);
  }

  public AnnotationAttributes(Map<String, Object> map, String annotationName) {
    this(new ArrayList<>(map.size() * 2));
    this.annotationName = annotationName;
    putAll(map);
  }

  AnnotationAttributes(ArrayList<Object> values) {
    this.values = values;
  }

  public AnnotationAttributes(AnnotationAttributes other) {
    this.values = new ArrayList<>(other.values);
    this.annotationType = other.annotationType;
    this.annotationName = other.annotationName;
    this.size = other.size;
  }

  @SuppressWarnings("unchecked")
  public Class<? extends Annotation> annotationType() {
    if (annotationType == null && !Objects.equals(annotationName, UNKNOWN)) {
      try {
        annotationType = Class.forName(annotationName);
      }
      catch (ClassNotFoundException ignored) {
      }
    }
    return this.annotationType;
  }

  public boolean contains(Collection<Class<? extends Annotation>> annotationToScan) {
    final String annotationName = annotationName();
    if (annotationName != null) {
      for (final Class<?> aClass : annotationToScan) {
        if (annotationName.equals(aClass.getName())) {
          return true;
        }
      }
      return false;
    }
    else if (annotationType != null) {
      return annotationToScan.contains(annotationType);
    }
    return false;
  }

  public boolean isTarget(Class<?> targetType) {
    final String annotationName = annotationName();
    if (annotationName != null) {
      return annotationName.equals(targetType.getName());
    }
    return targetType == annotationType;
  }

  public String annotationName() {
    return annotationName;
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
    Object attributeValue = get(attributeName); // get value

    // @since 4.0
    if (attributeValue instanceof List) {
      // more than two values
      final List<T> list = (List<T>) attributeValue;
      if (expectedType.isArray()) {
        // target return type is array
        Class<?> expectedComponentType = expectedType.getComponentType();
        final Object[] array = (Object[]) Array.newInstance(expectedComponentType, list.size());
        int i = 0;
        for (final Object target : list) {
          Object realValue = getRealValue(target);
          if (expectedComponentType.isInstance(realValue)) {
            array[i++] = realValue;
          }
          else {
            return null;
          }
        }
        put(attributeName, array); // replace
        return (T) array;
      }
      else if (!expectedType.isInstance(attributeValue)) {
        // single value
        final T ret = (T) getRealValue(list.get(0));
        if (expectedType.isInstance(ret)) {
          list.set(0, ret);
          return ret;
        }
        // not a target value
        return null;
      }
    }

    if (attributeValue == null) {
      return null;
    }

    // @since 4.0
    attributeValue = getRealValue(attributeValue);
    if (attributeValue != null && !expectedType.isInstance(attributeValue)) {
      // is not a target instance
      if (expectedType.isArray()) {
        // return type is array but target attr is not an array
        if (expectedType.getComponentType().isInstance(attributeValue)) {
          Object array = Array.newInstance(expectedType.getComponentType(), 1);
          Array.set(array, 0, attributeValue);
          attributeValue = array;
        }
      }
      else {
        // return type is not an array but target attr is an array
        if (attributeValue.getClass().isArray()) {
          // use first element
          attributeValue = Array.get(attributeValue, 0);
        }
      }
    }
    return (T) attributeValue;
  }

  private Object getRealValue(Object target) {
    if (target instanceof AnnotationValueHolder) {
      target = ((AnnotationValueHolder) target).read();
    }
    return target;
  }

  /**
   * @since 4.0
   */
  @Override
  public Object get(Object name) {
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

  /**
   * @since 4.0
   */
  @Override
  public Object put(String key, Object value) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    // key - value
    for (int i = 0; i < size; i += 2) {
      if (Objects.equals(values.get(i), key)) {
        // replace
        Object old = values.get(i + 1);
        values.set(i + 1, value);
        return old;
      }
    }
    this.size++;
    values.add(key);
    values.add(value);
    return null;
  }

  /**
   * append values if exist
   *
   * @since 4.0
   */
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

  /**
   * @since 4.0
   */
  @Override
  public void putAll(Map<? extends String, ?> attributes) {
    ArrayList<Object> thisValues = this.values;
    int addSize = 0;
    for (final Map.Entry<? extends String, ?> entry : attributes.entrySet()) {
      thisValues.add(entry.getKey());
      thisValues.add(entry.getValue());
      addSize++;
    }
    this.size += addSize;
  }

  /**
   * @since 4.0
   */
  public void putAll(AnnotationAttributes attributes) {
    if (attributes.isEmpty()) {
      return;
    }
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

  /**
   * @since 4.0
   */
  public void addAll(Map<String, Object> attributes) {
    ArrayList<Object> thisValues = this.values;
    for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
      thisValues.add(entry.getKey());
      thisValues.add(entry.getValue());
    }
  }

  /**
   * @since 4.0
   */
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
  @Override
  public Object remove(Object name) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i += 2) {
      Object key = values.get(i);
      if (Objects.equals(key, name)) {
        Object ret = values.get(i + 1);
        values.remove(i);
        values.remove(i);
        this.size--;
        return ret;
      }
    }
    return null;
  }

  /**
   * @since 4.0
   */
  @Override
  public void forEach(BiConsumer<? super String, ? super Object> action) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i++) {
      action.accept((String) values.get(i++), values.get(i));
    }
  }

  /**
   * @since 4.0
   */
  @Override
  public Set<Entry<String, Object>> entrySet() {
    return toMap().entrySet();
  }

  /**
   * @since 4.0
   */
  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<>();
    copyToMap(map);
    return map;
  }

  /**
   * @since 4.0
   */
  public void copyToMap(Map<String, Object> map) {
    ArrayList<Object> values = this.values;
    int size = values.size();
    for (int i = 0; i < size; i++) {
      map.put((String) values.get(i++), values.get(i));
    }
  }

  /**
   * Trims the capacity of this <tt>ArrayList</tt> instance to be the
   * list's current size.  An application can use this operation to minimize
   * the storage of an <tt>ArrayList</tt> instance.
   *
   * @since 4.0
   */
  public void trimToSize() {
    values.trimToSize();
  }

  /**
   * @since 4.0
   */
  public int size() {
    return size;
  }

  /**
   * @since 4.0
   */
  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * @since 4.0
   */
  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  /**
   * @since 4.0
   */
  @Override
  public boolean containsValue(Object value) {
    ArrayList<Object> thisValues = this.values;
    int size = thisValues.size();
    for (int i = 0; i < size; i += 2) {
      if (Objects.equals(thisValues.get(i + 1), value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @since 4.0
   */
  @Override
  public void clear() {
    values.clear();
    size = 0;
  }

  /**
   * @since 4.0
   */
  @Override
  public Set<String> keySet() {
    LinkedHashSet<String> ketSet = new LinkedHashSet<>();
    ArrayList<Object> thisValues = this.values;
    int size = thisValues.size();
    for (int i = 0; i < size; i += 2) {
      ketSet.add((String) thisValues.get(i));
    }
    return ketSet;
  }

  /**
   * @since 4.0
   */
  @Override
  public Collection<Object> values() {
    ArrayList<Object> values = new ArrayList<>();
    ArrayList<Object> thisValues = this.values;
    int size = thisValues.size();
    for (int i = 0; i < size; i += 2) {
      values.add(thisValues.get(i + 1));
    }
    return values;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(annotationName + " {");
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
              && Objects.equals(annotationName, other.annotationName)
              && Objects.equals(values, other.values);
    }
    return false;
  }

  @Override
  public int getOrder() {
    return OrderUtils.getOrder(annotationType);
  }

  // package

  void setAnnotationType(Class annotationType) {
    this.annotationType = annotationType;
  }

  void setAnnotationName(String annotationName) {
    this.annotationName = annotationName;
  }

}
