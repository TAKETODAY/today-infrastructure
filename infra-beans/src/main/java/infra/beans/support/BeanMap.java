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
 * A <code>Map</code>-based view of a JavaBean. The default set of keys is the
 * union of all property names. if ignoreReadOnly == true, an attempt to set a
 * read-only property will be ignored. Removal of objects is not a supported
 * (the key set is fixed).
 *
 * @param <T> target bean type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #ignoreReadOnly
 * @since 3.0.2 2021/5/28 21:15
 */
public final class BeanMap<T> extends AbstractMap<String, Object> {

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
   * @throws NotWritablePropertyException If this property is read only
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
   * @throws NoSuchPropertyException If no such property
   * @throws NotWritablePropertyException If this property is read only and 'ignoreReadOnly' is false
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
   * Return the underlying bean that this map operates on.
   *
   * @return the target bean
   */
  public T getTarget() {
    return target;
  }

  /**
   * Set the underlying bean that this map operates on.
   *
   * @param target the new target bean
   */
  public void setTarget(T target) {
    this.target = target;
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
   * Create a new <code>BeanMap</code> instance using the specified bean. This is
   * faster than using the {@link #forInstance(Object)} static method.
   *
   * @param bean the JavaBean underlying the map
   * @return a new <code>BeanMap</code> instance
   */
  public BeanMap<T> withInstance(T bean) {
    return new BeanMap<>(bean, metadata);
  }

  /**
   * Create a new instance of the underlying bean type, and set it as this map's
   * {@link #getTarget() target}.
   *
   * @return the newly created target bean
   */
  @SuppressWarnings("unchecked")
  public T newInstance() {
    T instance = (T) metadata.newInstance();
    setTarget(instance);
    return instance;
  }

  /**
   * Set whether attempts to set a read-only property should be silently ignored.
   *
   * @param ignoreReadOnly {@code true} to ignore read-only properties
   * @see #isIgnoreReadOnly()
   */
  public void setIgnoreReadOnly(boolean ignoreReadOnly) {
    this.ignoreReadOnly = ignoreReadOnly;
  }

  /**
   * Return whether attempts to set a read-only property are silently ignored.
   *
   * @return {@code true} if read-only properties are ignored
   * @see #setIgnoreReadOnly(boolean)
   */
  public boolean isIgnoreReadOnly() {
    return ignoreReadOnly;
  }

  // static

  /**
   * Create a new {@code BeanMap} for the given bean instance.
   *
   * @param bean the bean to wrap
   * @param <T> the bean type
   * @return a new {@code BeanMap}
   */
  public static <T> BeanMap<T> forInstance(T bean) {
    return new BeanMap<>(bean, BeanMetadata.forInstance(bean));
  }

  /**
   * Create a new {@code BeanMap} for the given bean class, backed by a new
   * instance of that class.
   *
   * @param beanClass the bean class
   * @param <T> the bean type
   * @return a new {@code BeanMap}
   */
  @SuppressWarnings("unchecked")
  public static <T> BeanMap<T> forClass(Class<T> beanClass) {
    BeanMetadata metadata = BeanMetadata.forClass(beanClass);
    return new BeanMap<>((T) metadata.newInstance(), metadata);
  }

}
