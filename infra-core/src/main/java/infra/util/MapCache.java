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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import infra.lang.Assert;
import infra.lang.NullValue;

/**
 * A thread-safe map-based cache implementation that supports lazy initialization of values.
 * It provides methods to retrieve cached values based on keys, with optional parameters
 * for value computation when the key is not present in the cache.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of cached values
 * @param <P> The type of additional parameter used during value computation
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/1/27 23:02
 */
public class MapCache<K, V extends @Nullable Object, P extends @Nullable Object> {

  private final Map<K, V> mapping;

  /**
   * default mapping function
   */
  private final @Nullable Function<K, V> mappingFunction;

  /**
   * Constructs a new instance with default initial capacity and load factor.
   */
  public MapCache() {
    this(new HashMap<>());
  }

  /**
   * Constructs a new instance with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity of the underlying map
   */
  public MapCache(int initialCapacity) {
    this(new HashMap<>(initialCapacity));
  }

  /**
   * Constructs a new instance with the specified map as the underlying storage.
   *
   * @param mapping allows to define your own map implementation
   */
  public MapCache(Map<K, V> mapping) {
    this.mapping = mapping;
    this.mappingFunction = null;
  }

  /**
   * Constructs a new instance with default map implementation and the specified mapping function.
   *
   * @param mappingFunction the default function to compute values when keys are not present
   */
  public MapCache(Function<K, V> mappingFunction) {
    this(new HashMap<>(), mappingFunction);
  }

  /**
   * Constructs a new instance with the specified map implementation and mapping function.
   *
   * @param mapping allows to define your own map implementation
   * @param mappingFunction the default function to compute values when keys are not present
   */
  public MapCache(Map<K, V> mapping, @Nullable Function<K, V> mappingFunction) {
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
   * @param k key with which the specified value is to be associated
   * @param p createValue's param
   * @return the current (existing or computed) value associated with
   * the specified key, should never {@code null}
   * @see #createValue
   */
  public final @NonNull V get(K k, P p) {
    V v = mapping.get(k);
    if (v == null) {
      synchronized(mapping) {
        v = mapping.get(k);
        if (v == null) {
          v = createValue(k, p);
          Assert.state(v != null, "createValue() returns null");
          mapping.put(k, v);
        }
      }
    }
    return v;
  }

  /**
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   *
   * @param k key with which the specified value is to be associated
   * @return the current (existing or computed) value associated with
   * the specified key, or null if the computed value is null
   */
  public final V get(K k) {
    return get(k, mappingFunction);
  }

  /**
   * If the specified key is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this map unless {@code null}.
   *
   * @param k key with which the specified value is to be associated
   * @param mappingFunction the function to compute a value, can be null,
   * if its null use default mappingFunction
   * @return the current (existing or computed) value associated with
   * the specified key, or null if the computed value is null
   */
  @SuppressWarnings("unchecked")
  public final V get(K k, @Nullable Function<K, V> mappingFunction) {
    V v = mapping.get(k);
    if (v == null) {
      synchronized(mapping) {
        v = mapping.get(k);
        if (v == null) {
          if (mappingFunction == null) {
            mappingFunction = this.mappingFunction;
          }
          if (mappingFunction != null) {
            v = mappingFunction.apply(k);
          }
          else {
            // fallback to #createValue()
            v = createValue(k, null);
          }
          if (v == null) {
            v = (V) NullValue.INSTANCE;
          }
          mapping.put(k, v);
        }
      }
    }
    return unwrap(v);
  }

  protected V createValue(K k, P p) {
    return null;
  }

  public @Nullable V put(K k, @Nullable V v) {
    synchronized(mapping) {
      return unwrap(mapping.put(k, v));
    }
  }

  public void clear() {
    synchronized(mapping) {
      mapping.clear();
    }
  }

  public @Nullable V remove(K k) {
    synchronized(mapping) {
      return unwrap(mapping.remove(k));
    }
  }

  private static <V extends @Nullable Object> @Nullable V unwrap(@Nullable V ret) {
    return ret == NullValue.INSTANCE ? null : ret;
  }

}
