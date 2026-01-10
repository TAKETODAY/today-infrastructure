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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import infra.core.ArraySizeTrimmer;
import infra.lang.Assert;

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

  protected final transient Function<K, List<V>> mappingFunction;

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
    targetMap.computeIfAbsent(key, mappingFunction)
            .add(value);
  }

  @Override
  public void addAll(K key, @Nullable Collection<? extends V> values) {
    if (CollectionUtils.isNotEmpty(values)) {
      targetMap.computeIfAbsent(key, mappingFunction)
              .addAll(values);
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
  @Nullable
  public List<V> setOrRemove(K key, @Nullable V value) {
    if (value != null) {
      List<V> values = mappingFunction.apply(key);
      values.add(value);
      return targetMap.put(key, values);
    }
    else {
      return targetMap.remove(key);
    }
  }

  @Nullable
  @Override
  public List<V> setOrRemove(K key, V @Nullable [] value) {
    if (value != null) {
      List<V> values = mappingFunction.apply(key);
      CollectionUtils.addAll(values, value);
      return targetMap.put(key, values);
    }
    return targetMap.remove(key);
  }

  @Nullable
  @Override
  public List<V> setOrRemove(K key, @Nullable Collection<V> value) {
    if (value != null) {
      List<V> values = mappingFunction.apply(key);
      CollectionUtils.addAll(values, value);
      return targetMap.put(key, values);
    }
    return targetMap.remove(key);
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
