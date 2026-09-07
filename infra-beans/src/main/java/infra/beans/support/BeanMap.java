/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.NoSuchPropertyException;
import infra.beans.NotWritablePropertyException;
import infra.core.Pair;
import infra.reflect.SetterMethod;

/**
 * A {@link java.util.Map Map}-based view of a JavaBean's properties.
 *
 * <p>The keys are fixed by the bean's metadata and cannot be added or removed.
 * Calling {@link #put(String, Object)} writes the corresponding property on the
 * underlying bean, while {@link #remove(Object)} and {@link #clear()} are not
 * supported.
 *
 * <p>A {@code BeanMap} is permanently associated with its target bean and its
 * read-only property handling policy. Use {@link #withInstance(Object)} to
 * obtain a map for another bean or {@link #withIgnoreReadOnly(boolean)} to use a
 * different policy. This does not make the underlying bean immutable: its
 * properties may still be changed through this map or directly on the bean.
 *
 * <p>By default, attempting to write a read-only property raises a
 * {@link NotWritablePropertyException}. This can be changed to silently ignore
 * such writes through {@link #withIgnoreReadOnly(boolean)}.
 *
 * @param <T> target bean type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanMetadata
 * @see BeanProperty
 * @since 3.0.2 2021/5/28 21:15
 */
public final class BeanMap<T> extends AbstractMap<String, Object> {

  private final T target;

  private final BeanMetadata metadata;

  private final boolean ignoreReadOnly;

  private BeanMap(T target, BeanMetadata metadata, boolean ignoreReadOnly) {
    this.target = target;
    this.metadata = metadata;
    this.ignoreReadOnly = ignoreReadOnly;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    Object target = this.target;
    LinkedHashSet<Entry<String, Object>> entrySet = new LinkedHashSet<>();
    for (BeanProperty property : metadata) {
      entrySet.add(Pair.of(property.getName(), property.getValue(target)));
    }
    return entrySet;
  }

  @Override
  public Set<String> keySet() {
    return metadata.propertyNames();
  }

  /**
   * Returns the value of the property identified by the given key.
   *
   * @param key the property name
   * @return the property value, or {@code null} if the property is not set or
   * cannot be read
   * @throws IllegalArgumentException if the given key is not a {@link String}
   */
  @Override
  public @Nullable Object get(Object key) {
    if (key instanceof String name) {
      return metadata.getPropertyValue(target, name);
    }
    throw new IllegalArgumentException("key must be a string");
  }

  /**
   * Returns the value of the property identified by the given key from the given
   * target object, without changing this map's {@link #getTarget() target}.
   *
   * @param target the target bean to read from
   * @param key the property name
   * @return the property value, or {@code null} if the property is not set or
   * cannot be read
   */
  public @Nullable Object get(Object target, String key) {
    return metadata.getPropertyValue(target, key);
  }

  /**
   * Sets the value of the property identified by the given key on the
   * {@link #getTarget() target} object.
   *
   * @param key the property name
   * @param value the value to set
   * @return the previous value of the property, or {@code null} if there was none
   * or the property is not readable
   * @throws NoSuchPropertyException if no such property exists
   * @throws NotWritablePropertyException if the property is read-only and
   * {@link #isIgnoreReadOnly()} is {@code false}
   * @see SetterMethod#set(Object, Object)
   */
  @Override
  public @Nullable Object put(String key, Object value) {
    return put(target, key, value);
  }

  /**
   * Sets the value of the property identified by the given key on the given target
   * object, without changing this map's {@link #getTarget() target}.
   *
   * @param target the target bean to write to
   * @param key the property name
   * @param value the value to set
   * @return the previous value of the property, or {@code null} if there was none
   * or the property is not readable
   * @throws NoSuchPropertyException if no such property exists
   * @throws NotWritablePropertyException if the property is read-only and
   * {@link #isIgnoreReadOnly()} is {@code false}
   * @see SetterMethod#set(Object, Object)
   */
  public @Nullable Object put(Object target, String key, Object value) {
    BeanProperty beanProperty = this.metadata.obtainBeanProperty(key);
    if (beanProperty.isWriteable()) {
      Object old = beanProperty.isReadable() ? beanProperty.getValue(target) : null;
      beanProperty.setValue(target, value);
      return old;
    }
    if (!ignoreReadOnly) {
      throw new NotWritablePropertyException(metadata.getType(), beanProperty.getName(),
              "%s has a property: '%s' that is not-writeable".formatted(target, beanProperty.getName()));
    }
    return beanProperty.getValue(target);
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof String name && metadata.containsProperty(name);
  }

  @Override
  public int size() {
    return metadata.getPropertyCount();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * @throws UnsupportedOperationException always, since the key set is fixed
   */
  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always, since the key set is fixed
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Return the underlying bean that this map operates on. The returned reference
   * remains the same for the lifetime of this map.
   *
   * @return the target bean
   */
  public T getTarget() {
    return target;
  }

  /**
   * Get the type of a property.
   *
   * @param name the name of the JavaBean property
   * @return the type of the property, or null if the property does not exist
   */
  public @Nullable Class<?> getPropertyType(String name) {
    BeanProperty beanProperty = metadata.getProperty(name);
    return beanProperty != null ? beanProperty.getType() : null;
  }

  /**
   * Create a new {@code BeanMap} for the specified bean, reusing this map's
   * metadata and read-only property handling policy. This is faster than using
   * {@link #forInstance(Object)} when the bean has the same type as this map's
   * target.
   *
   * @param bean the JavaBean underlying the map
   * @return a new {@code BeanMap} bound to the specified bean
   */
  public BeanMap<T> withInstance(T bean) {
    return new BeanMap<>(bean, metadata, ignoreReadOnly);
  }

  /**
   * Create a new {@code BeanMap} bound to a fresh instance of the underlying bean
   * type. The new map inherits this map's read-only property handling policy.
   *
   * @return a new {@code BeanMap} bound to a newly created target bean
   */
  @SuppressWarnings("unchecked")
  public BeanMap<T> newInstance() {
    return withInstance((T) metadata.newInstance());
  }

  /**
   * Return a {@code BeanMap} configured to either ignore or reject attempts to
   * set read-only properties. The returned map is bound to the same target bean
   * and reuses the same metadata as this map.
   *
   * @param ignoreReadOnly {@code true} to ignore read-only properties
   * @return a new {@code BeanMap} with the requested setting, or this instance
   * if the setting is unchanged
   * @since 5.0
   */
  public BeanMap<T> withIgnoreReadOnly(boolean ignoreReadOnly) {
    return this.ignoreReadOnly == ignoreReadOnly
            ? this : new BeanMap<>(target, metadata, ignoreReadOnly);
  }

  /**
   * Return whether attempts to set a read-only property are silently ignored.
   *
   * @return {@code true} if read-only properties are ignored
   * @see #withIgnoreReadOnly(boolean)
   */
  public boolean isIgnoreReadOnly() {
    return ignoreReadOnly;
  }

  // static

  /**
   * Create a new {@code BeanMap} for the given bean instance. Attempts to write
   * read-only properties are rejected by default.
   *
   * @param bean the bean to wrap
   * @param <T> the bean type
   * @return a new {@code BeanMap}
   */
  public static <T> BeanMap<T> forInstance(T bean) {
    return new BeanMap<>(bean, BeanMetadata.forInstance(bean), false);
  }

  /**
   * Create a new {@code BeanMap} for the given bean class, backed by a new
   * instance of that class. Attempts to write read-only properties are rejected
   * by default.
   *
   * @param beanClass the bean class
   * @param <T> the bean type
   * @return a new {@code BeanMap}
   */
  @SuppressWarnings("unchecked")
  public static <T> BeanMap<T> forClass(Class<T> beanClass) {
    BeanMetadata metadata = BeanMetadata.forClass(beanClass);
    return new BeanMap<>((T) metadata.newInstance(), metadata, false);
  }

}
