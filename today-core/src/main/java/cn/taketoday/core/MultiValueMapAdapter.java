/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Adapts a given {@link Map} to the {@link MultiValueMap} contract.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LinkedMultiValueMap
 * @since 4.0 2022/4/25 14:39
 */
@SuppressWarnings("serial")
public class MultiValueMapAdapter<K, V> implements MultiValueMap<K, V>, Serializable {

  private final Map<K, List<V>> targetMap;

  /**
   * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
   *
   * @param targetMap the plain target {@code Map}
   */
  public MultiValueMapAdapter(Map<K, List<V>> targetMap) {
    Assert.notNull(targetMap, "'targetMap' must not be null");
    this.targetMap = targetMap;
  }

  // MultiValueMap implementation

  @Override
  @Nullable
  public V getFirst(K key) {
    List<V> values = this.targetMap.get(key);
    return (values != null && !values.isEmpty() ? values.get(0) : null);
  }

  @Override
  public void add(K key, @Nullable V value) {
    List<V> values = this.targetMap.computeIfAbsent(key, k -> new ArrayList<>(1));
    values.add(value);
  }

  @Override
  public void addAll(K key, @Nullable Collection<? extends V> values) {
    if (values != null) {
      List<V> currentValues = this.targetMap.computeIfAbsent(key, k -> new ArrayList<>(1));
      currentValues.addAll(values);
    }
  }

  @Override
  public void set(K key, @Nullable V value) {
    List<V> values = new ArrayList<>(1);
    values.add(value);
    this.targetMap.put(key, values);
  }

  // Map implementation

  @Override
  public int size() {
    return this.targetMap.size();
  }

  @Override
  public boolean isEmpty() {
    return this.targetMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return this.targetMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return this.targetMap.containsValue(value);
  }

  @Override
  @Nullable
  public List<V> get(Object key) {
    return this.targetMap.get(key);
  }

  @Override
  @Nullable
  public List<V> put(K key, List<V> value) {
    return this.targetMap.put(key, value);
  }

  @Override
  @Nullable
  public List<V> remove(Object key) {
    return this.targetMap.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends List<V>> map) {
    this.targetMap.putAll(map);
  }

  @Override
  public void clear() {
    this.targetMap.clear();
  }

  @Override
  public Set<K> keySet() {
    return this.targetMap.keySet();
  }

  @Override
  public Collection<List<V>> values() {
    return this.targetMap.values();
  }

  @Override
  public Set<Entry<K, List<V>>> entrySet() {
    return this.targetMap.entrySet();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || this.targetMap.equals(other));
  }

  @Override
  public int hashCode() {
    return this.targetMap.hashCode();
  }

  @Override
  public String toString() {
    return this.targetMap.toString();
  }

}
