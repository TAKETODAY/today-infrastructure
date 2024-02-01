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

package cn.taketoday.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Simple implementation of {@link MultiValueMap} that wraps a {@link Map},
 * storing multiple values in a {@link List}. Can Specify a {@link #mappingFunction}
 * to determine which List you use , default is {@link ArrayList}
 *
 * <p>
 * This Map implementation is generally not thread-safe. It is primarily
 * designed for data structures exposed from request objects, for use in a
 * single thread only.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2020-01-27 13:15
 * @since 2.1.7
 */
@SuppressWarnings({ "unchecked" })
public class DefaultMultiValueMap<K, V> implements MultiValueMap<K, V>, Serializable, Cloneable, ArraySizeTrimmer {

  @Serial
  private static final long serialVersionUID = 1L;

  protected final Map<K, List<V>> map;
  protected transient final Function<K, List<V>> mappingFunction;

  public DefaultMultiValueMap() {
    this(new HashMap<>());
  }

  public DefaultMultiValueMap(Function<K, List<V>> mappingFunction) {
    this.map = new HashMap<>();
    this.mappingFunction = mappingFunction;
  }

  public DefaultMultiValueMap(int initialCapacity) {
    this(new HashMap<>(initialCapacity));
  }

  public DefaultMultiValueMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, defaultMappingFunction);
  }

  public DefaultMultiValueMap(int initialCapacity, float loadFactor, Function<K, List<V>> mappingFunction) {
    this.map = new HashMap<>(initialCapacity, loadFactor);
    this.mappingFunction = mappingFunction;
  }

  /**
   * adaptation
   */
  public DefaultMultiValueMap(Map<K, List<V>> map) {
    this.map = map;
    this.mappingFunction = defaultMappingFunction;
  }

  public DefaultMultiValueMap(Map<K, List<V>> map, Function<K, List<V>> mappingFunction) {
    this.map = map;
    this.mappingFunction = mappingFunction;
  }

  public DefaultMultiValueMap(Map<K, List<V>> map, Function<K, List<V>> mappingFunction, boolean copy) {
    this.map = copy ? new HashMap<>(map) : map;
    this.mappingFunction = mappingFunction;
  }

  // MultiValueMap
  // -------------------------------------------------

  @Override
  public V getFirst(K key) {
    List<V> values = this.map.get(key);
    return (values != null && !values.isEmpty() ? values.get(0) : null);
  }

  @Override
  public void add(K key, @Nullable V value) {
    List<V> values = this.map.computeIfAbsent(key, mappingFunction);
    values.add(value);
  }

  @Override
  public void addAll(K key, @Nullable Collection<? extends V> values) {
    if (values != null) {
      List<V> currentValues = this.map.computeIfAbsent(key, mappingFunction);
      currentValues.addAll(values);
    }
  }

  /**
   * @param key they key
   * @param values the values to be added
   * @since 4.0
   */
  @Override
  public void addAll(K key, @Nullable Enumeration<? extends V> values) {
    if (values != null) {
      List<V> currentValues = this.map.computeIfAbsent(key, mappingFunction);
      CollectionUtils.addAll(currentValues, values);
      CollectionUtils.trimToSize(currentValues);
    }
  }

  @Override
  public void set(K key, @Nullable V value) {
    List<V> values = mappingFunction.apply(key);
    values.add(value);
    this.map.put(key, values);
  }

  /**
   * Trims the capacity of this map internal value <tt>ArrayList</tt> instance to be the
   * list's current size.  An application can use this operation to minimize
   * the storage of an <tt>ArrayList</tt> instance.
   *
   * @see ArrayList#trimToSize()
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    for (Entry<K, List<V>> entry : map.entrySet()) {
      CollectionUtils.trimToSize(entry.getValue());
    }
  }

  // Map
  // ----------------------------------

  @Override
  public int size() {
    return this.map.size();
  }

  @Override
  public boolean isEmpty() {
    return this.map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return this.map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return this.map.containsValue(value);
  }

  @Override
  @Nullable
  public List<V> get(Object key) {
    return this.map.get(key);
  }

  @Override
  @Nullable
  public List<V> put(K key, List<V> value) {
    return this.map.put(key, value);
  }

  @Override
  @Nullable
  public List<V> remove(Object key) {
    return this.map.remove(key);
  }

  @Override
  public void putAll(@NonNull Map<? extends K, ? extends List<V>> map) {
    this.map.putAll(map);
  }

  @Override
  public void clear() {
    this.map.clear();
  }

  @Override
  public Set<K> keySet() {
    return this.map.keySet();
  }

  @Override
  public Collection<List<V>> values() {
    return this.map.values();
  }

  @Override
  public Set<Entry<K, List<V>>> entrySet() {
    return this.map.entrySet();
  }

  /**
   * Create a deep copy of this Map.
   *
   * @return a copy of this Map, including a copy of each value-holding List entry
   * (consistently using an independent modifiable {@link List} for
   * each entry) along the lines of {@code MultiValueMap.addAll} semantics
   * @see #addAll(Map)
   * @see #clone()
   * @since 2.1.7
   */
  public DefaultMultiValueMap<K, V> deepCopy() {
    DefaultMultiValueMap<K, V> ret = new DefaultMultiValueMap<>(map.size());
    for (Entry<K, List<V>> entry : map.entrySet()) {
      K key = entry.getKey();
      List<V> value = entry.getValue();

      List<V> apply = mappingFunction.apply(key);
      apply.addAll(value);
      ret.put(key, apply);
    }
    return ret;
  }

  /**
   * Create a regular copy of this Map.
   *
   * @return a shallow copy of this Map, reusing this Map's value-holding List
   * entries (even if some entries are shared or unmodifiable) along the
   * lines of standard {@code Map.put} semantics
   * @see #put(Object, List)
   * @see #putAll(Map)
   * @see DefaultMultiValueMap#DefaultMultiValueMap(Map)
   * @see #deepCopy()
   * @since 4.0
   */
  @Override
  public DefaultMultiValueMap<K, V> clone() {
    return cloneMap();
  }

  /**
   * @since 4.0
   */
  public DefaultMultiValueMap<K, V> cloneMap() {
    return new DefaultMultiValueMap<>(this, mappingFunction, true);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || map.equals(obj);
  }

  @Override
  public int hashCode() {
    return this.map.hashCode();
  }

  @Override
  public String toString() {
    return this.map.toString();
  }

}
