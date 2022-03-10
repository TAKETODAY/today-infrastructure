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

package cn.taketoday.cache.support;

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;

/**
 * Common base class for {@link Cache} implementations that need to adapt
 * {@code null} values (and potentially other such special values) before
 * passing them on to the underlying store.
 *
 * <p>Transparently replaces given {@code null} user values with an internal
 * {@link NullValue#INSTANCE}, if configured to support {@code null} values
 * (as indicated by {@link #isAllowNullValues()}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 20:45
 */
public abstract class AbstractValueAdaptingCache implements Cache {

  private final boolean allowNullValues;

  /**
   * Create an {@code AbstractValueAdaptingCache} with the given setting.
   *
   * @param allowNullValues whether to allow for {@code null} values
   */
  protected AbstractValueAdaptingCache(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

  /**
   * Return whether {@code null} values are allowed in this cache.
   */
  public final boolean isAllowNullValues() {
    return this.allowNullValues;
  }

  @Override
  @Nullable
  public ValueWrapper get(Object key) {
    return toValueWrapper(lookup(key));
  }

  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T get(Object key, @Nullable Class<T> type) {
    Object value = fromStoreValue(lookup(key));
    if (value != null && type != null && !type.isInstance(value)) {
      throw new IllegalStateException(
              "Cached value is not of required type [" + type.getName() + "]: " + value);
    }
    return (T) value;
  }

  /**
   * Perform an actual lookup in the underlying store.
   *
   * @param key the key whose associated value is to be returned
   * @return the raw store value for the key, or {@code null} if none
   */
  @Nullable
  protected abstract Object lookup(Object key);

  /**
   * Convert the given value from the internal store to a user value
   * returned from the get method (adapting {@code null}).
   *
   * @param storeValue the store value
   * @return the value to return to the user
   */
  @Nullable
  protected Object fromStoreValue(@Nullable Object storeValue) {
    if (this.allowNullValues && storeValue == NullValue.INSTANCE) {
      return null;
    }
    return storeValue;
  }

  /**
   * Convert the given user value, as passed into the put method,
   * to a value in the internal store (adapting {@code null}).
   *
   * @param userValue the given user value
   * @return the value to store
   */
  protected Object toStoreValue(@Nullable Object userValue) {
    if (userValue == null) {
      if (this.allowNullValues) {
        return NullValue.INSTANCE;
      }
      throw new IllegalArgumentException(
              "Cache '" + getName() + "' is configured to not allow null values but null was provided");
    }
    return userValue;
  }

  /**
   * Wrap the given store value with a {@link SimpleValueWrapper}, also going
   * through {@link #fromStoreValue} conversion. Useful for {@link #get(Object)}
   * and {@link #putIfAbsent(Object, Object)} implementations.
   *
   * @param storeValue the original value
   * @return the wrapped value
   */
  @Nullable
  protected Cache.ValueWrapper toValueWrapper(@Nullable Object storeValue) {
    return storeValue != null ? new SimpleValueWrapper(fromStoreValue(storeValue)) : null;
  }

}
