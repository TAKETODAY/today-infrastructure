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

package cn.taketoday.beans.support;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.beans.factory.PropertyReadOnlyException;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.core.Assert;

/**
 * A <code>Map</code>-based view of a JavaBean. The default set of keys is the
 * union of all property names. if ignoreReadOnly == true, an attempt to set a
 * read-only property will be ignored. Removal of objects is not a supported
 * (the key set is fixed).
 *
 * @author TODAY 20w21/5/28 21:15
 * @see #ignoreReadOnly
 * @since 3.0.2
 */
public final class BeanMapping<T> extends AbstractMap<String, Object> implements Map<String, Object> {
  private T target;
  private final BeanMetadata metadata;

  /**
   * throws a PropertyReadOnlyException when set a read-only property
   */
  private boolean ignoreReadOnly;

  public BeanMapping(T target) {
    this.target = target;
    this.metadata = BeanMetadata.ofObject(target);
  }

  public BeanMapping(BeanMetadata metadata) {
    this.metadata = metadata;
  }

  BeanMapping(T target, BeanMetadata metadata) {
    this.target = target;
    this.metadata = metadata;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    final Object target = obtainTarget();
    final LinkedHashSet<Entry<String, Object>> entrySet = new LinkedHashSet<>();
    for (final Entry<String, BeanProperty> entry : metadata.getBeanProperties().entrySet()) {
      final String key = entry.getKey();
      final Object value = entry.getValue().getValue(target);
      entrySet.add(new Node<>(key, value));
    }
    return entrySet;
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(metadata.getBeanProperties().keySet());
  }

  static final class Node<K, V> implements Map.Entry<K, V> {
    final K key;
    V value;

    Node(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public K getKey() { return key; }

    public V getValue() { return value; }

    public String toString() { return key + "=" + value; }

    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    public V setValue(V newValue) {
      V oldValue = value;
      value = newValue;
      return oldValue;
    }

    public boolean equals(Object o) {
      if (o == this)
        return true;
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
        return Objects.equals(key, e.getKey()) &&
                Objects.equals(value, e.getValue());
      }
      return false;
    }
  }

  @Override
  public Object get(Object key) {
    if (key instanceof String) {
      return get(obtainTarget(), (String) key);
    }
    throw new IllegalArgumentException("key must be a string");
  }

  public Object get(Object target, String key) {
    return metadata.getProperty(target, key);
  }

  /**
   * @throws PropertyReadOnlyException
   *         If this property is read only
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   */
  @Override
  public Object put(String key, Object value) {
    return put(obtainTarget(), key, value);
  }

  /**
   * @throws NoSuchPropertyException
   *         If no such property
   * @throws PropertyReadOnlyException
   *         If this property is read only and 'ignoreReadOnly' is false
   * @see cn.taketoday.core.reflect.SetterMethod#set(Object, Object)
   */
  public Object put(Object target, String key, Object value) {
    final BeanProperty beanProperty = this.metadata.obtainBeanProperty(key);
    if (beanProperty.isReadOnly()) {
      if (!ignoreReadOnly) {
        throw new PropertyReadOnlyException(
                target + " has a property: '" + beanProperty.getName() + "' that is read-only");
      }
    }
    else {
      final Object property = beanProperty.getValue(target);
      beanProperty.setValue(target, value);
      return property;
    }
    return beanProperty.getValue(target);
  }

  @Override
  public boolean containsKey(Object key) {
    return keySet().contains(key);
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    if (o != this) {
      if (!(o instanceof Map)) {
        return false;
      }
      Map other = (Map) o;
      if (size() != other.size()) {
        return false;
      }
      for (final Object key : keySet()) {
        if (!Objects.equals(get(key), other.get(key))) {
          return false;
        }
      }
    }
    return true;
  }

  public Object getTarget() {
    return target;
  }

  private Object obtainTarget() {
    final Object target = getTarget();
    Assert.state(target != null, "No target");
    return target;
  }

  public void setTarget(T target) {
    this.target = target;
  }

  /**
   * Get the type of a property.
   *
   * @param name
   *         the name of the JavaBean property
   *
   * @return the type of the property, or null if the property does not exist
   */
  public Class<?> getPropertyType(String name) {
    final BeanProperty beanProperty = metadata.getBeanProperty(name);
    if (beanProperty != null) {
      return beanProperty.getType();
    }
    return null;
  }

  /**
   * Create a new <code>BeanMapping</code> instance using the specified bean. This is
   * faster than using the {@link #ofObject(Object)} static method.
   *
   * @param bean
   *         the JavaBean underlying the map
   *
   * @return a new <code>BeanMapping</code> instance
   */
  public BeanMapping<T> newInstance(T bean) {
    return new BeanMapping<>(bean, metadata);
  }

  @SuppressWarnings("unchecked")
  public T newInstance() {
    final T instance = (T) metadata.newInstance();
    setTarget(instance);
    return instance;
  }

  public void setIgnoreReadOnly(boolean ignoreReadOnly) {
    this.ignoreReadOnly = ignoreReadOnly;
  }

  public boolean isIgnoreReadOnly() {
    return ignoreReadOnly;
  }

  // static

  public static <T> BeanMapping<T> ofObject(T bean) {
    return new BeanMapping<>(bean);
  }

  @SuppressWarnings("unchecked")
  public static <T> BeanMapping<T> ofClass(Class<T> beanClass) {
    final BeanMetadata metadata = BeanMetadata.ofClass(beanClass);
    return new BeanMapping<>((T) metadata.newInstance(), metadata);
  }

}
