/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

import infra.lang.Assert;
import infra.lang.Unmodifiable;

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

  Function smartListMappingFunction = k -> new SmartList<>();

  // read only
  MultiValueMap EMPTY = forAdaption(Collections.emptyMap());

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

  default void add(Entry<K, V> pair) {
    add(pair.getKey(), pair.getValue());
  }

  /**
   * Add all the values of the given list to the current list of values for the
   * given key.
   *
   * @param key they key
   * @param values the values to be added
   * @see ObjectUtils#isNotEmpty(Object[])
   * @since 4.0
   */
  default void addAll(K key, @Nullable V[] values) {
    if (ObjectUtils.isNotEmpty(values)) {
      for (V element : values) {
        add(key, element);
      }
    }
  }

  /**
   * Add all the values of the given list to the current list of values for the
   * given key.
   *
   * @param key they key
   * @param values the values to be added
   * @see CollectionUtils#isNotEmpty(Collection)
   * @since 4.0
   */
  default void addAll(K key, @Nullable Collection<? extends V> values) {
    if (CollectionUtils.isNotEmpty(values)) {
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
    if (CollectionUtils.isNotEmpty(values)) {
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
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for the key, the old
   * value is replaced.
   *
   * <p> remove the value if value is {@code null}.
   *
   * @param key the key
   * @param value the value, or {@code null} for none
   * @return the previous value associated with {@code key}, or
   * {@code null} if there was no mapping for {@code key}.
   * (A {@code null} return can also indicate that the map
   * previously associated {@code null} with {@code key}.)
   * @see #remove(Object)
   * @since 5.0
   */
  @Nullable
  List<V> setOrRemove(K key, @Nullable V value);

  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for the key, the old
   * value is replaced.
   *
   * <p> remove the value if value is {@code null}.
   *
   * @param key they key
   * @param value the values
   * @see Map#put(Object, Object)
   * @see #remove(Object)
   * @since 5.0
   */
  @Nullable
  List<V> setOrRemove(K key, @Nullable V[] value);

  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for the key, the old
   * value is replaced.
   *
   * <p> remove the value if value is {@code null}.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with {@code key}, or
   * {@code null} if there was no mapping for {@code key}.
   * (A {@code null} return can also indicate that the map
   * previously associated {@code null} with {@code key}.)
   * @see Map#put(Object, Object)
   * @see #remove(Object)
   * @since 5.0
   */
  @Nullable
  List<V> setOrRemove(K key, @Nullable Collection<V> value);

  /**
   * Null check for {@link #putAll(Map)}
   *
   * @param values the values.
   * @see #putAll(Map)
   */
  default void setAll(@Nullable Map<K, List<V>> values) {
    if (CollectionUtils.isNotEmpty(values)) {
      putAll(values);
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

  /**
   * Apply a read-only {@code MultiValueMap} wrapper around this {@code MultiValueMap}, if necessary.
   *
   * @return a read-only variant of the MultiValueMap, or the original map as-is
   * @since 4.0
   */
  default MultiValueMap<K, V> asReadOnly() {
    return new UnmodifiableMultiValueMap<>(this);
  }

  /**
   * Remove any read-only wrapper that may have been previously applied around
   * this map via {@link #asReadOnly()}.
   *
   * @return a writable variant of the MultiValueMap, or the original headers as-is
   * @since 4.0
   */
  default MultiValueMap<K, V> asWritable() {
    return this;
  }

  // static

  @SuppressWarnings({ "unchecked" })
  @Unmodifiable
  static <K, V> MultiValueMap<K, V> empty() {
    return EMPTY;
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @return the adapted multi-value map (wrapping the original map)
   * @since 4.0
   */
  static <K, V> MultiValueMapAdapter<K, V> forAdaption(Map<K, List<V>> targetMap) {
    return new MultiValueMapAdapter<>(targetMap);
  }

  /**
   * Adapt a {@code HashMap<K, List<V>>} to an {@code MultiValueMap<K, V>} with a
   * list value mapping function.
   *
   * @param <K> key
   * @param <V> value type
   * @return MappingMultiValueMap
   */
  static <K, V> MappingMultiValueMap<K, V> forAdaption(Function<K, List<V>> mappingFunction) {
    return new MappingMultiValueMap<>(mappingFunction);
  }

  /**
   * Adapt a {@code HashMap<K, List<V>>} to an {@code MultiValueMap<K, V>} with a
   * smart list value mapping function.
   *
   * @param <K> key
   * @param <V> value type
   * @return MappingMultiValueMap
   */
  @SuppressWarnings("unchecked")
  static <K, V> MappingMultiValueMap<K, V> forSmartListAdaption(Map<K, List<V>> targetMap) {
    return new MappingMultiValueMap<>(targetMap, smartListMappingFunction);
  }

  /**
   * Adapt a {@code HashMap<K, List<V>>} to an {@code MultiValueMap<K, V>} with a
   * smart list value mapping function.
   *
   * @param <K> key
   * @param <V> value type
   * @return MappingMultiValueMap
   */
  @SuppressWarnings("unchecked")
  static <K, V> MappingMultiValueMap<K, V> forSmartListAdaption() {
    return new MappingMultiValueMap<>(smartListMappingFunction);
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @param mappingFunction list mapping function
   * @return the adapted multi-value map (wrapping the original map)
   * @since 4.0
   */
  static <K, V> MappingMultiValueMap<K, V> forAdaption(Map<K, List<V>> targetMap, Function<K, List<V>> mappingFunction) {
    return new MappingMultiValueMap<>(targetMap, mappingFunction);
  }

  /**
   * Copy a {@code Map<K, List<V>>} to an {@code LinkedMultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @return the copied LinkedMultiValueMap
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> copyOf(Map<K, List<V>> targetMap) {
    return new LinkedMultiValueMap<>(targetMap);
  }

  /**
   * Copy a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @return the copied multi-value map
   * @since 5.0
   */
  static <K, V> MappingMultiValueMap<K, V> copyOf(Function<K, List<V>> mappingFunction, Map<K, List<V>> targetMap) {
    MappingMultiValueMap<K, V> map = forAdaption(mappingFunction);
    map.addAll(targetMap);
    return map;
  }

  /**
   * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
   *
   * @param targetMap the original map
   * @return the adapted multi-value map (wrapping the original map)
   * @since 5.0
   */
  static <K, V> MappingMultiValueMap<K, V> copyOf(Map<K, List<V>> targetMap, Function<K, List<V>> mappingFunction, Map<K, List<V>> source) {
    MappingMultiValueMap<K, V> map = forAdaption(targetMap, mappingFunction);
    map.addAll(source);
    return map;
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
   * @param expectedSize the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @since 4.0
   */
  static <K, V> LinkedMultiValueMap<K, V> forLinkedHashMap(int expectedSize) {
    return new LinkedMultiValueMap<>(expectedSize);
  }

}
