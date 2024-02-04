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
import java.util.function.Function;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.Assert;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.1.7 2020-01-27 13:15
 */
@SuppressWarnings({ "unchecked" })
public class MappingMultiValueMap<K, V> extends MultiValueMapAdapter<K, V>
        implements MultiValueMap<K, V>, Serializable, ArraySizeTrimmer {

  @Serial
  private static final long serialVersionUID = 1L;

  protected transient final Function<K, List<V>> mappingFunction;

  /**
   * Wrap the {@link HashMap} as a {@link MultiValueMap} adapter.
   */
  public MappingMultiValueMap() {
    this(new HashMap<>());
  }

  /**
   * Wrap the {@link HashMap} as a {@link MultiValueMap} adapter.
   *
   * @param mappingFunction list value mapping function
   */
  public MappingMultiValueMap(Function<K, List<V>> mappingFunction) {
    this(new HashMap<>(), mappingFunction);
  }

  /**
   * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
   *
   * @param map the plain target {@code Map}
   */
  public MappingMultiValueMap(Map<K, List<V>> map) {
    super(map);
    this.mappingFunction = defaultMappingFunction;
  }

  /**
   * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
   *
   * @param map the plain target {@code Map}
   * @param mappingFunction list value mapping function
   */
  public MappingMultiValueMap(Map<K, List<V>> map, Function<K, List<V>> mappingFunction) {
    super(map);
    Assert.notNull(mappingFunction, "mappingFunction is required");
    this.mappingFunction = mappingFunction;
  }

  // MultiValueMap
  // -------------------------------------------------

  @Override
  public void add(K key, @Nullable V value) {
    List<V> values = targetMap.computeIfAbsent(key, mappingFunction);
    values.add(value);
  }

  @Override
  public void addAll(K key, @Nullable Collection<? extends V> values) {
    if (values != null) {
      List<V> currentValues = targetMap.computeIfAbsent(key, mappingFunction);
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
      List<V> currentValues = targetMap.computeIfAbsent(key, mappingFunction);
      CollectionUtils.addAll(currentValues, values);
      CollectionUtils.trimToSize(currentValues);
    }
  }

  @Override
  public void set(K key, @Nullable V value) {
    List<V> values = mappingFunction.apply(key);
    values.add(value);
    targetMap.put(key, values);
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
    for (Entry<K, List<V>> entry : targetMap.entrySet()) {
      CollectionUtils.trimToSize(entry.getValue());
    }
  }

}
