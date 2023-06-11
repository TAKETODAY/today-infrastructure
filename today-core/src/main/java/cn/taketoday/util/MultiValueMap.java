/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Extension of the {@code Map} interface that stores multiple values.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.1.7 2020-01-27 13:06
 */
@SuppressWarnings({ "rawtypes" })
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

  Function defaultMappingFunction = k -> new ArrayList<>(1);

  /**
   * Return the first value for the given key.
   *
   * @param key the key
   * @return the first value for the specified key, or {@code null} if none
   */
  @Nullable
  V getFirst(K key);

  /**
   * Add the given single value to the current list of values for the given key.
   *
   * @param key the key
   * @param value the value to be added
   */
  void add(K key, @Nullable V value);

  /**
   * Add all the values of the given list to the current list of values for the
   * given key.
   *
   * @param key they key
   * @param values the values to be added
   * @since 4.0
   */
  default void addAll(K key, @Nullable Collection<? extends V> values) {
    if (values != null) {
      for (V element : values) {
        add(key, element);
      }
    }
  }

  /**
   * Add all the values of the given enumeration to the current enumeration of values for the
   * given key.
   *
   * @param key they key
   * @param values the values to be added
   * @since 4.0
   */
  default void addAll(K key, @Nullable Enumeration<? extends V> values) {
    if (values != null) {
      while (values.hasMoreElements()) {
        V element = values.nextElement();
        add(key, element);
      }
    }
  }

  /**
   * Add all the values of the given {@code MultiValueMap} to the current values.
   *
   * @param values the values to be added
   */
  default void addAll(@Nullable Map<K, List<V>> values) {
    if (values != null) {
      for (Entry<K, List<V>> entry : values.entrySet()) {
        addAll(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Add all the values of the given {@code MultiValueMap} to the current values.
   *
   * @param pair Entry
   * @since 4.0
   */
  default void addAll(Entry<K, ? extends Collection<V>> pair) {
    addAll(pair.getKey(), pair.getValue());
  }

  /**
   * {@link #add(Object, Object) Add} the given value, only when the map does not
   * {@link #containsKey(Object) contain} the given key.
   *
   * @param key the key
   * @param value the value to be added
   */
  default void addIfAbsent(K key, @Nullable V value) {
    if (!containsKey(key)) {
      add(key, value);
    }
  }

  /**
   * Set the given single value under the given key.
   *
   * @param key the key
   * @param value the value to set
   */
  void set(K key, @Nullable V value);

  /**
   * Set the given values under.
   *
   * @param values the values.
   */
  default void setAll(@Nullable Map<K, V> values) {
    if (values != null) {
      for (Entry<K, V> entry : values.entrySet()) {
        set(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Return a {@code Map} with the first values contained in this
   * {@code MultiValueMap}.
   *
   * @return a single value representation of this map
   */
  default Map<K, V> toSingleValueMap() {
    LinkedHashMap<K, V> singleValueMap = CollectionUtils.newLinkedHashMap(size());
    for (Entry<K, List<V>> entry : entrySet()) {
      List<V> values = entry.getValue();
      if (CollectionUtils.isNotEmpty(values)) {
        singleValueMap.put(entry.getKey(), values.get(0));
      }
    }
    return singleValueMap;
  }

  /**
   * @since 3.0
   */
  default Map<K, V[]> toArrayMap(IntFunction<V[]> function) {
    LinkedHashMap<K, V[]> singleValueMap = CollectionUtils.newLinkedHashMap(size());
    copyToArrayMap(singleValueMap, function);
    return singleValueMap;
  }

  /**
   * @since 3.0
   */
  default void copyToArrayMap(Map<K, V[]> newMap, IntFunction<V[]> mappingFunction) {
    Assert.notNull(newMap, "newMap is required");
    Assert.notNull(mappingFunction, "mappingFunction is required");
    for (Entry<K, List<V>> entry : entrySet()) {
      List<V> values = entry.getValue();
      if (CollectionUtils.isNotEmpty(values)) {
        V[] toArray = values.toArray(mappingFunction.apply(values.size()));
        newMap.put(entry.getKey(), toArray);
      }
    }
  }

  // static

  /**
   * default MultiValueMap
   *
   * @since 4.0
   */
  static <K, V> DefaultMultiValueMap<K, V> defaults() {
    return new DefaultMultiValueMap<>();
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @return the adapted multi-value map (wrapping the original map)
   * @since 4.0
   */
  static <K, V> DefaultMultiValueMap<K, V> from(Map<K, List<V>> targetMap) {
    return new DefaultMultiValueMap<>(targetMap);
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @param mappingFunction list mapping function
   * @return the adapted multi-value map (wrapping the original map)
   * @since 4.0
   */
  static <K, V> DefaultMultiValueMap<K, V> from(
          Map<K, List<V>> targetMap, Function<K, List<V>> mappingFunction) {
    return new DefaultMultiValueMap<>(targetMap, mappingFunction);
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @return the adapted multi-value map (wrapping the original map)
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> copyOf(Map<K, List<V>> targetMap) {
    return new LinkedMultiValueMap<>(targetMap);
  }

  static <K, V> LinkedMultiValueMap<K, V> copyOf(Map<K, List<V>> targetMap, Function<K, List<V>> mappingFunction) {
    return new LinkedMultiValueMap<>(targetMap, mappingFunction);
  }

  /**
   * Adapt a {@code LinkedHashMap<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> forLinkedHashMap() {
    return new LinkedMultiValueMap<>();
  }

  /**
   * Adapt a {@code LinkedHashMap<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> forLinkedHashMap(Function<K, List<V>> mappingFunction) {
    return new LinkedMultiValueMap<>(mappingFunction);
  }

  /**
   * Adapt a {@code LinkedHashMap<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param expectedSize the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> forLinkedHashMap(int expectedSize) {
    return new LinkedMultiValueMap<>(expectedSize);
  }

  /**
   * Adapt a {@code LinkedHashMap<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param expectedSize the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> forLinkedHashMap(
          int expectedSize, Function<K, List<V>> mappingFunction) {
    return new LinkedMultiValueMap<>(expectedSize, mappingFunction);
  }

  /**
   * Return an unmodifiable view of the specified multi-value map.
   *
   * @param targetMap the map for which an unmodifiable view is to be returned.
   * @return an unmodifiable view of the specified multi-value map
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  static <K, V> MultiValueMap<K, V> forUnmodifiable(MultiValueMap<? extends K, ? extends V> targetMap) {
    Assert.notNull(targetMap, "'targetMap' is required");
    if (targetMap instanceof UnmodifiableMultiValueMap) {
      return (MultiValueMap<K, V>) targetMap;
    }
    return new UnmodifiableMultiValueMap<>(targetMap);
  }

}
