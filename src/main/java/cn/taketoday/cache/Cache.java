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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;

/**
 * Abstraction for that defines common cache operations.
 *
 * <b>Note:</b> Due to the generic use of caching, it is recommended that
 * implementations allow storage of <tt>null</tt> values (for example to
 * cache methods that return {@code null}).
 *
 * @author TODAY 2019-02-27 17:11
 */
public abstract class Cache {
  private String name = Constant.DEFAULT;

  public void setName(String name) {
    Assert.notNull(name, "Name must not be null");
    this.name = name;
  }

  /**
   * Return the cache name.
   */
  public String getName() {
    return name;
  }

  /**
   * Return the value to which this cache maps the specified key.
   * <p>
   * Returns {@code null} if the cache contains no mapping for this key;
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which this cache maps the specified key, A straight
   * {@code null} being returned means that the cache contains no mapping
   * for this key or the key is map to a {@code null} value.
   * @see #get(Object, Class)
   * @see #get(Object, boolean)
   * @see #toRealValue(Object)
   */
  @Nullable
  public Object get(final Object key) {
    return get(key, true);
  }

  /**
   * Return the value to which this cache maps the specified key.
   * <p>
   * Returns {@code null} if the cache contains no mapping for this key;
   *
   * @param key the key whose associated value is to be returned
   * @param unWarp unWarp value, if its {@code true} un-warp store value to real value
   * @return the value to which this cache maps the specified key, A straight
   * {@code null} being returned means that the cache contains no mapping
   * for this key. if returns {@link NullValue#INSTANCE} indicates that
   * the key maps to a {@code null} value
   * @see #get(Object, Class)
   * @see NullValue#INSTANCE
   * @see NullValue
   * @see #toRealValue(Object)
   */
  @Nullable
  public Object get(final Object key, final boolean unWarp) {
    final Object userValue = doGet(key);
    return unWarp ? toRealValue(userValue) : userValue;
  }

  /**
   * look up value in mappings with given key
   *
   * @param key given key
   * @return cached value maybe a warped value
   */
  @Nullable
  protected abstract Object doGet(Object key);

  /**
   * Return the value to which this cache maps the specified key, generically
   * specifying a type that return value will be cast to.
   * <p>
   * Note: This variant of {@code get} does not allow for differentiating between
   * a cached {@code null} value and no cache entry found at all. Use the standard
   * {@link #get(Object)} variant for that purpose instead.
   *
   * @param key the key whose associated value is to be returned
   * @param requiredType the required type of the returned value (may be {@code null} to
   * bypass a type check; in case of a {@code null} value found in the
   * cache, the specified type is irrelevant)
   * @return the value to which this cache maps the specified key (which may be
   * {@code null} itself), or also {@code null} if the cache contains no
   * mapping for this key
   * @throws IllegalStateException if a cache entry has been found but failed to match the specified
   * type
   * @see #get(Object)
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T get(final Object key, final Class<T> requiredType) {
    final Object value = get(key, true);
    if (value != null && requiredType != null && !requiredType.isInstance(value)) {
      throw new IllegalStateException(
              "Cached value is not of required type [" + requiredType.getName() + "]: " + value);
    }
    return (T) value;
  }

  /**
   * Return the value to which this cache maps the specified key, obtaining that
   * value from {@code valueLoader} if necessary. This method provides a simple
   * substitute for the conventional "if cached, return; otherwise create, cache
   * and return" pattern.
   * <p>
   * If possible, implementations should ensure that the loading operation is
   * synchronized so that the specified {@code valueLoader} is only called once in
   * case of concurrent access on the same key.
   * <p>
   *
   * @param key the key whose associated value is to be returned
   * @param valueLoader Value Loader
   * @return the value to which this cache maps the specified key
   * @throws CacheValueRetrievalException If cache value failed to load
   * @see #get(Object, boolean)
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, CacheCallback<T> valueLoader) {
    return (T) toRealValue(computeIfAbsent(key, valueLoader));
  }

  /**
   * Get value If there isn't a key, use valueLoader create one
   *
   * @param <T> Cache value type
   * @param key Cache key
   * @param valueLoader Value Loader
   * @return cached value maybe a warped value
   * @throws CacheValueRetrievalException If CacheCallback#call() throws Exception
   */
  @Nullable
  protected <T> Object computeIfAbsent(Object key, CacheCallback<T> valueLoader) {
    Object ret = doGet(key);
    if (ret == null) {
      ret = computeValue(key, valueLoader);
      doPut(key, ret);
    }
    return ret;
  }

  /**
   * compute cache value from {@code valueLoader}
   *
   * @param valueLoader Cache value loader
   * @param <T> Value type
   * @return a value from {@code valueLoader} maybe warp to a {@code NullValue.INSTANCE}
   * when {@code valueLoader} returns {@code null}
   */
  protected final <T> Object computeValue(Object key, CacheCallback<T> valueLoader) {
    try {
      return toStoreValue(valueLoader.call());
    }
    catch (Throwable e) {
      throw new CacheValueRetrievalException(key, valueLoader, e);
    }
  }

  /**
   * Associate the specified value with the specified key in this cache.
   * <p>
   * If the cache previously contained a mapping for this key, the old value is
   * replaced by the specified value.
   *
   * @param key the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   */
  public void put(final Object key, @Nullable Object value) {
    doPut(key, toStoreValue(value));
  }

  /**
   * Atomically associate the specified value with the specified key in this cache
   * if it is not set already.
   * <p>This is equivalent to:
   * <pre><code>
   * Object existingValue = doGet(key);
   * if (existingValue == null) {
   *     cache.put(key, value);
   * }
   * return toRealValue(existingValue);
   * </code></pre>
   * except that the action is performed atomically. While all out-of-the-box
   * {@link CacheManager} implementations are able to perform the put atomically,
   * the operation may also be implemented in two steps, e.g. with a check for
   * presence and a subsequent put, in a non-atomic way. Check the documentation
   * of the native cache implementation that you are using for more details.
   * <p>The default implementation delegates to {@link #get(Object)} and
   * {@link #put(Object, Object)} along the lines of the code snippet above.
   *
   * @param key the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   * @return the value to which this cache maps the specified key (which may be
   * {@code null} itself), or also {@code null} if the cache did not contain any
   * mapping for that key prior to this call. Returning {@code null} is therefore
   * an indicator that the given {@code value} has been associated with the key.
   * @see #put(Object, Object)
   * @since 4.0
   */
  @Nullable
  public Object putIfAbsent(Object key, @Nullable Object value) {
    Object existingValue = doGet(key);
    if (existingValue == null) {
      put(key, value);
    }
    return toRealValue(existingValue);
  }

  /**
   * Put to this cache internal
   *
   * @param key Target key
   * @param value Target value
   */
  protected abstract void doPut(Object key, Object value);

  /**
   * Evict the mapping for this key from this cache if it is present.
   *
   * @param key the key whose mapping is to be removed from the cache
   */
  public abstract void evict(Object key);

  /**
   * Remove all mappings from the cache.
   */
  public abstract void clear();

  // static

  /**
   * real value to store value
   *
   * @param userValue real value
   * @return Store value maybe a serializable object
   */
  public static Object toStoreValue(final Object userValue) {
    return userValue == null ? NullValue.INSTANCE : userValue;
  }

  /**
   * convert store value to real value
   *
   * @param cachedValue cached value in mappings
   * @return if {@code cachedValue} is {@link NullValue#INSTANCE}
   * indicates that real value is {@code null}
   */
  @Nullable
  public static Object toRealValue(final Object cachedValue) {
    return cachedValue == NullValue.INSTANCE ? null : cachedValue;
  }

}
