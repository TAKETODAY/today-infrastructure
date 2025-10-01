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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import infra.lang.Assert;
import infra.lang.NullValue;

/**
 * Map cache
 *
 * @param <Key> key type
 * @param <Param> param type, extra computing param type
 * @param <Value> value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/1/27 23:02
 */
public class MapCache<Key, Value, Param> {

  private final Map<Key, Value> mapping;

  /** default mapping function */
  @Nullable
  private final Function<Key, @Nullable Value> mappingFunction;

  public MapCache() {
    this(new HashMap<>());
  }

  public MapCache(int initialCapacity) {
    this(new HashMap<>(initialCapacity));
  }

  /**
   * @param mapping allows to define your own map implementation
   */
  public MapCache(Map<Key, Value> mapping) {
    this.mapping = mapping;
    this.mappingFunction = null;
  }

  public MapCache(Function<Key, @Nullable Value> mappingFunction) {
    this(new HashMap<>(), mappingFunction);
  }

  /**
   * @param mapping allows to define your own map implementation
   */
  public MapCache(Map<Key, Value> mapping, @Nullable Function<Key, @Nullable Value> mappingFunction) {
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
   * the specified key, should never {@code null}
   * @see #createValue
   */
  public final Value get(Key key, Param param) {
    Value value = mapping.get(key);
    if (value == null) {
      synchronized(mapping) {
        value = mapping.get(key);
        if (value == null) {
          value = createValue(key, param);
          Assert.state(value != null, "createValue() returns null");
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
  @Nullable
  public final Value get(Key key) {
    return get(key, mappingFunction);
  }

  /**
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   *
   * @param key key with which the specified value is to be associated
   * @param mappingFunction the function to compute a value, can be null,
   * if its null use default mappingFunction
   * @return the current (existing or computed) value associated with
   * the specified key, or null if the computed value is null
   */
  @Nullable
  @SuppressWarnings({ "unchecked", "NullAway" })
  public final Value get(Key key, @Nullable Function<Key, Value> mappingFunction) {
    Value value = mapping.get(key);
    if (value == null) {
      synchronized(mapping) {
        value = mapping.get(key);
        if (value == null) {
          if (mappingFunction == null) {
            mappingFunction = this.mappingFunction;
          }
          if (mappingFunction != null) {
            value = mappingFunction.apply(key);
          }
          else {
            // fallback to #createValue()
            value = createValue(key, null);
          }
          if (value == null) {
            value = (Value) NullValue.INSTANCE;
          }
          mapping.put(key, value);
        }
      }
    }
    return unwrap(value);
  }

  @Nullable
  protected Value createValue(Key key, Param param) {
    return null;
  }

  @Nullable
  public Value put(Key key, @Nullable Value value) {
    synchronized(mapping) {
      return unwrap(mapping.put(key, value));
    }
  }

  public void clear() {
    synchronized(mapping) {
      mapping.clear();
    }
  }

  @Nullable
  public Value remove(Key key) {
    synchronized(mapping) {
      return unwrap(mapping.remove(key));
    }
  }

  @Nullable
  private static <V> V unwrap(@Nullable V ret) {
    return ret == NullValue.INSTANCE ? null : ret;
  }

}
