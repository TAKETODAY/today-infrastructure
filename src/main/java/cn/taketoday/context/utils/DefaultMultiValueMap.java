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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple implementation of {@link MultiValueMap} that wraps a {@link Map},
 * storing multiple values in a {@link LinkedList}.
 *
 * <p>
 * This Map implementation is generally not thread-safe. It is primarily
 * designed for data structures exposed from request objects, for use in a
 * single thread only.
 *
 * @param <K>
 *         the key type
 * @param <V>
 *         the value element type
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2020-01-27 13:15
 * @since 2.1.7
 */
public class DefaultMultiValueMap<K, V> implements MultiValueMap<K, V>, Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  private final Map<K, List<V>> map;

  public DefaultMultiValueMap() {
    this(new HashMap<>());
  }

  public DefaultMultiValueMap(int initialCapacity) {
    this(new HashMap<>(initialCapacity));
  }

  public DefaultMultiValueMap(int initialCapacity, float loadFactor) {
    this(new HashMap<>(initialCapacity, loadFactor));
  }

  public DefaultMultiValueMap(Map<K, List<V>> map) {
    this(map, false);
  }

  public DefaultMultiValueMap(Map<K, List<V>> map, boolean copy) {
    this.map = copy ? new HashMap<>(map) : map;
  }

  // MultiValueMap
  // -------------------------------------------------

  @Override
  public V getFirst(K key) {
    List<V> values = this.map.get(key);
    return (values != null && !values.isEmpty() ? values.get(0) : null);
  }

  @Override
  public void add(K key, V value) {
    List<V> values = this.map.computeIfAbsent(key, k -> new LinkedList<>());
    values.add(value);
  }

  @Override
  public void addAll(K key, List<? extends V> values) {
    List<V> currentValues = this.map.computeIfAbsent(key, k -> new LinkedList<>());
    currentValues.addAll(values);
  }

  @Override
  public void addAll(MultiValueMap<K, V> values) {
    for (Entry<K, List<V>> entry : values.entrySet()) {
      addAll(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void set(K key, V value) {
    List<V> values = new LinkedList<>();
    values.add(value);
    this.map.put(key, values);
  }

  @Override
  public void setAll(Map<K, V> values) {
    values.forEach(this::set);
  }

  @Override
  public Map<K, V> toSingleValueMap() {
    final HashMap<K, V> singleValueMap = new HashMap<>(map.size());
    for (final Entry<K, List<V>> entry : map.entrySet()) {
      final List<V> values = entry.getValue();
      if (!CollectionUtils.isEmpty(values)) {
        singleValueMap.put(entry.getKey(), values.get(0));
      }
    }
    return singleValueMap;
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
  public List<V> get(Object key) {
    return this.map.get(key);
  }

  @Override
  public List<V> put(K key, List<V> value) {
    return this.map.put(key, value);
  }

  @Override
  public List<V> remove(Object key) {
    return this.map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends List<V>> map) {
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
   * (consistently using an independent modifiable {@link LinkedList} for
   * each entry) along the lines of {@code MultiValueMap.addAll} semantics
   *
   * @see #addAll(MultiValueMap)
   * @see #clone()
   * @since 2.1.7
   */
  public DefaultMultiValueMap<K, V> deepCopy() {
    DefaultMultiValueMap<K, V> ret = new DefaultMultiValueMap<>(this.map.size());
    this.map.forEach((key, value) -> ret.put(key, new LinkedList<>(value)));
    return ret;
  }

  /**
   * Create a regular copy of this Map.
   *
   * @return a shallow copy of this Map, reusing this Map's value-holding List
   * entries (even if some entries are shared or unmodifiable) along the
   * lines of standard {@code Map.put} semantics
   *
   * @see #put(Object, List)
   * @see #putAll(Map)
   * @see DefaultMultiValueMap#DefaultMultiValueMap(Map)
   * @see #deepCopy()
   * @since 2.1.7
   */
  @Override
  public DefaultMultiValueMap<K, V> clone() {
    return new DefaultMultiValueMap<>(this, true);
  }

  @Override
  public boolean equals(Object obj) {
    return this.map.equals(obj);
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
