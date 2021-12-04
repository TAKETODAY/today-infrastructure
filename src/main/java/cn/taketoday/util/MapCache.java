/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.lang.Nullable;

/**
 * Map cache
 *
 * @param <K> key type
 * @param <T> param type
 * @param <V> value type
 * @author TODAY 2021/1/27 23:02
 * @since 3.0
 */
public class MapCache<K, V, T> {
  private final Map<K, V> mapping;
  /** default mapping function */
  private volatile Function<K, V> mappingFunction;

  public MapCache() {
    this(new HashMap<>());
  }

  public MapCache(int initialCapacity) {
    this(new HashMap<>(initialCapacity));
  }

  /**
   * @param mapping allows to define your own map implementation
   */
  public MapCache(Map<K, V> mapping) {
    this.mapping = mapping;
  }

  public MapCache(Function<K, V> mappingFunction) {
    this(new HashMap<>(), mappingFunction);
  }

  /**
   * @param mapping allows to define your own map implementation
   */
  public MapCache(Map<K, V> mapping, Function<K, V> mappingFunction) {
    this.mapping = mapping;
    this.mappingFunction = mappingFunction;
  }

  /**
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   * <p>
   * High performance way
   * </p>
   *
   * @param key key with which the specified value is to be associated
   * @param param createValue's param
   * @return the current (existing or computed) value associated with
   * the specified key, or null if the computed value is null
   * @see #createValue(Object, T)
   */
  public final V get(K key, T param) {
    V value = mapping.get(key);
    if (value == null) {
      synchronized(mapping) {
        value = mapping.get(key);
        if (value == null) {
          value = createValue(key, param);
          mapping.put(key, value);
        }
      }
    }
    return value;
  }

  /**
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   *
   * @param key key with which the specified value is to be associated
   * @return the current (existing or computed) value associated with
   * the specified key, or null if the computed value is null
   */
  public final V get(K key) {
    return get(key, (Function<K, V>) null);
  }

  /**
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   *
   * @param key key with which the specified value is to be associated
   * @param mappingFunction the function to compute a value, can be null, if its null use default mappingFunction
   * @return the current (existing or computed) value associated with
   * the specified key, or null if the computed value is null
   */
  public final V get(K key, @Nullable Function<K, V> mappingFunction) {
    V value = mapping.get(key);
    if (value == null) {
      synchronized(mapping) {
        value = mapping.get(key);
        if (value == null) {
          if (mappingFunction == null) {
            mappingFunction = this.mappingFunction;
          }
          if (mappingFunction != null) {
            value = mappingFunction.apply(key);
            mapping.put(key, value);
          }
        }
      }
    }
    return value;
  }

  protected V createValue(K key, T param) {
    return null;
  }

  synchronized
  public void setMappingFunction(Function<K, V> mappingFunction) {
    this.mappingFunction = mappingFunction;
  }

  public V put(K key, V value) {
    synchronized(mapping) {
      return mapping.put(key, value);
    }
  }

  public void clear() {
    synchronized(mapping) {
      mapping.clear();
    }
  }

  public V remove(K key) {
    synchronized(mapping) {
      return mapping.remove(key);
    }
  }

}
