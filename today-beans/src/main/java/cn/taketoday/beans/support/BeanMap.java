/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.beans.support;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.NotWritablePropertyException;
import cn.taketoday.core.Pair;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.SetterMethod;
import cn.taketoday.util.ObjectUtils;

/**
 * A <code>Map</code>-based view of a JavaBean. The default set of keys is the
 * union of all property names. if ignoreReadOnly == true, an attempt to set a
 * read-only property will be ignored. Removal of objects is not a supported
 * (the key set is fixed).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #ignoreReadOnly
 * @since 3.0.2 2021/5/28 21:15
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class BeanMap<T> extends AbstractMap<String, Object> implements Map<String, Object> {

  private T target;

  private final BeanMetadata metadata;

  /**
   * throws a NotWritablePropertyException when set a read-only property
   */
  private boolean ignoreReadOnly;

  private BeanMap(T target, BeanMetadata metadata) {
    this.target = target;
    this.metadata = metadata;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    Object target = this.target;
    LinkedHashSet<Entry<String, Object>> entrySet = new LinkedHashSet<>();
    for (BeanProperty property : metadata) {
      Object value = property.getValue(target);
      entrySet.add(Pair.of(property.getName(), value));
    }
    return entrySet;
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(metadata.getBeanProperties().keySet());
  }

  @Override
  public Object get(Object key) {
    if (key instanceof String) {
      return get(target, (String) key);
    }
    throw new IllegalArgumentException("key must be a string");
  }

  public Object get(Object target, String key) {
    return metadata.getProperty(target, key);
  }

  /**
   * @throws NotWritablePropertyException If this property is read only
   * @see SetterMethod#set(Object, Object)
   */
  @Override
  @Nullable
  public Object put(String key, Object value) {
    return put(target, key, value);
  }

  /**
   * @throws NoSuchPropertyException If no such property
   * @throws NotWritablePropertyException If this property is read only and 'ignoreReadOnly' is false
   * @see SetterMethod#set(Object, Object)
   */
  @Nullable
  public Object put(Object target, String key, Object value) {
    BeanProperty beanProperty = this.metadata.obtainBeanProperty(key);
    if (beanProperty.isWriteable()) {
      Object old = null;
      if (beanProperty.isReadable()) {
        old = beanProperty.getValue(target);
      }
      beanProperty.setValue(target, value);
      return old;
    }
    else {
      if (!ignoreReadOnly) {
        throw new NotWritablePropertyException(metadata.getType(), beanProperty.getName(),
                "%s has a property: '%s' that is not-writeable".formatted(target, beanProperty.getName()));
      }
    }
    return beanProperty.getValue(target);
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof String) {
      return metadata.containsProperty((String) key);
    }
    return false;
  }

  @Override
  public int size() {
    return metadata.getPropertySize();
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
      if (o instanceof BeanMap other) {
        // is BeanMap
        return ObjectUtils.nullSafeEquals(target, other.target)
                && Objects.equals(metadata, other.metadata);
      }
    }
    return true;
  }

  public T getTarget() {
    return target;
  }

  public void setTarget(T target) {
    this.target = target;
  }

  /**
   * Get the type of a property.
   *
   * @param name the name of the JavaBean property
   * @return the type of the property, or null if the property does not exist
   */
  public Class<?> getPropertyType(String name) {
    BeanProperty beanProperty = metadata.getBeanProperty(name);
    if (beanProperty != null) {
      return beanProperty.getType();
    }
    return null;
  }

  /**
   * Create a new <code>BeanMap</code> instance using the specified bean. This is
   * faster than using the {@link #forInstance(Object)} static method.
   *
   * @param bean the JavaBean underlying the map
   * @return a new <code>BeanMap</code> instance
   */
  public BeanMap<T> withInstance(T bean) {
    return new BeanMap<>(bean, metadata);
  }

  @SuppressWarnings("unchecked")
  public T newInstance() {
    T instance = (T) metadata.newInstance();
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

  public static <T> BeanMap<T> forInstance(T bean) {
    return new BeanMap<>(bean, BeanMetadata.forInstance(bean));
  }

  @SuppressWarnings("unchecked")
  public static <T> BeanMap<T> forClass(Class<T> beanClass) {
    BeanMetadata metadata = BeanMetadata.forClass(beanClass);
    return new BeanMap<>((T) metadata.newInstance(), metadata);
  }

}
