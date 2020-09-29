/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

/**
 * @author TODAY <br>
 * 2019-02-27 17:11
 */
public interface Cache {

  /**
   * Return the cache name.
   */
  String getName();

  /**
   * Return the value to which this cache maps the specified key.
   * <p>
   * Returns {@code null} if the cache contains no mapping for this key;
   *
   * @param key
   *         the key whose associated value is to be returned
   *
   * @return the value to which this cache maps the specified key, A straight
   * {@code null} being returned means that the cache contains no mapping
   * for this key.
   *
   * @see #get(Object, Class)
   */
  Object get(Object key);

  /**
   * Return the value to which this cache maps the specified key, generically
   * specifying a type that return value will be cast to.
   * <p>
   * Note: This variant of {@code get} does not allow for differentiating between
   * a cached {@code null} value and no cache entry found at all. Use the standard
   * {@link #get(Object)} variant for that purpose instead.
   *
   * @param key
   *         the key whose associated value is to be returned
   * @param targetType
   *         the required type of the returned value (may be {@code null} to
   *         bypass a type check; in case of a {@code null} value found in the
   *         cache, the specified type is irrelevant)
   *
   * @return the value to which this cache maps the specified key (which may be
   * {@code null} itself), or also {@code null} if the cache contains no
   * mapping for this key
   *
   * @throws IllegalStateException
   *         if a cache entry has been found but failed to match the specified
   *         type
   * @see #get(Object)
   */
  <T> T get(Object key, Class<T> targetType);

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
   * @param key
   *         the key whose associated value is to be returned
   * @param valueLoader
   *         Value Loader
   *
   * @return the value to which this cache maps the specified key
   *
   * @throws CacheValueRetrievalException
   *         If cache value failed to load
   */
  <T> T get(Object key, CacheCallback<T> valueLoader) throws CacheValueRetrievalException;

  /**
   * Associate the specified value with the specified key in this cache.
   * <p>
   * If the cache previously contained a mapping for this key, the old value is
   * replaced by the specified value.
   *
   * @param key
   *         the key with which the specified value is to be associated
   * @param value
   *         the value to be associated with the specified key
   */
  void put(Object key, Object value);

  /**
   * Evict the mapping for this key from this cache if it is present.
   *
   * @param key
   *         the key whose mapping is to be removed from the cache
   */
  void evict(Object key);

  /**
   * Remove all mappings from the cache.
   */
  void clear();

}
