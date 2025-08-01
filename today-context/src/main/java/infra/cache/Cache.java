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

package infra.cache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import infra.cache.annotation.Cacheable;
import infra.lang.Nullable;
import infra.util.function.ThrowingFunction;

/**
 * Interface that defines common cache operations.
 *
 * <p>Serves primarily as an SPI for Infra annotation-based caching
 * model ({@link Cacheable} and co)
 * and secondarily as an API for direct usage in applications.
 *
 * <p><b>Note:</b> Due to the generic use of caching, it is recommended
 * that implementations allow storage of {@code null} values
 * (for example to cache methods that return {@code null}).
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CacheManager
 * @see Cacheable
 * @since 2019-02-27 17:11
 */
public interface Cache {

  /**
   * Return the cache name.
   */
  String getName();

  /**
   * Return the underlying native cache provider.
   */
  Object getNativeCache();

  /**
   * Return the value to which this cache maps the specified key.
   * <p>Returns {@code null} if the cache contains no mapping for this key;
   * otherwise, the cached value (which may be {@code null} itself) will
   * be returned in a {@link ValueWrapper}.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which this cache maps the specified key,
   * contained within a {@link ValueWrapper} which may also hold
   * a cached {@code null} value. A straight {@code null} being
   * returned means that the cache contains no mapping for this key.
   * @see #get(Object, Class)
   * @see #get(Object, ThrowingFunction)
   */
  @Nullable
  ValueWrapper get(Object key);

  /**
   * Return the value to which this cache maps the specified key,
   * generically specifying a type that return value will be cast to.
   * <p>Note: This variant of {@code get} does not allow for differentiating
   * between a cached {@code null} value and no cache entry found at all.
   * Use the standard {@link #get(Object)} variant for that purpose instead.
   *
   * @param key the key whose associated value is to be returned
   * @param type the required type of the returned value (may be
   * {@code null} to bypass a type check; in case of a {@code null}
   * value found in the cache, the specified type is irrelevant)
   * @return the value to which this cache maps the specified key
   * (which may be {@code null} itself), or also {@code null} if
   * the cache contains no mapping for this key
   * @throws IllegalStateException if a cache entry has been found
   * but failed to match the specified type
   * @see #get(Object)
   * @since 4.0
   */
  @Nullable
  <T> T get(Object key, @Nullable Class<T> type);

  /**
   * Return the value to which this cache maps the specified key, obtaining
   * that value from {@code valueLoader} if necessary. This method provides
   * a simple substitute for the conventional "if cached, return; otherwise
   * create, cache and return" pattern.
   * <p>If possible, implementations should ensure that the loading operation
   * is synchronized so that the specified {@code valueLoader} is only called
   * once in case of concurrent access on the same key.
   * <p>If the {@code valueLoader} throws an exception, it is wrapped in
   * a {@link ValueRetrievalException}
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which this cache maps the specified key
   * @throws ValueRetrievalException if the {@code valueLoader} throws an exception
   * @see #get(Object)
   * @since 5.0
   */
  @Nullable
  <K, V> V get(K key, ThrowingFunction<? super K, ? extends V> valueLoader);

  /**
   * Return the value to which this cache maps the specified key,
   * wrapped in a {@link CompletableFuture}. This operation must not block
   * but is allowed to return a completed {@link CompletableFuture} if the
   * corresponding value is immediately available.
   * <p>Can return {@code null} if the cache can immediately determine that
   * it contains no mapping for this key (e.g. through an in-memory key map).
   * Otherwise, the cached value will be returned in the {@link CompletableFuture},
   * with {@code null} indicating a late-determined cache miss. A nested
   * {@link ValueWrapper} potentially indicates a nullable cached value;
   * the cached value may also be represented as a plain element if null
   * values are not supported. Calling code needs to be prepared to handle
   * all those variants of the result returned by this method.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which this cache maps the specified key, contained
   * within a {@link CompletableFuture} which may also be empty when a cache
   * miss has been late-determined. A straight {@code null} being returned
   * means that the cache immediately determined that it contains no mapping
   * for this key. A {@link ValueWrapper} contained within the
   * {@code CompletableFuture} indicates a cached value that is potentially
   * {@code null}; this is sensible in a late-determined scenario where a regular
   * CompletableFuture-contained {@code null} indicates a cache miss. However,
   * a cache may also return a plain value if it does not support the actual
   * caching of {@code null} values, avoiding the extra level of value wrapping.
   * Infra cache processing can deal with all such implementation strategies.
   * @see #retrieve(Object, Supplier)
   */
  @Nullable
  default CompletableFuture<?> retrieve(Object key) {
    throw new UnsupportedOperationException(
            getClass().getName() + " does not support CompletableFuture-based retrieval");
  }

  /**
   * Return the value to which this cache maps the specified key, obtaining
   * that value from {@code valueLoader} if necessary. This method provides
   * a simple substitute for the conventional "if cached, return; otherwise
   * create, cache and return" pattern, based on {@link CompletableFuture}.
   * This operation must not block.
   * <p>If possible, implementations should ensure that the loading operation
   * is synchronized so that the specified {@code valueLoader} is only called
   * once in case of concurrent access on the same key.
   * <p>Null values always indicate a user-level {@code null} value with this
   * method. The provided {@link CompletableFuture} handle produces a value
   * or raises an exception. If the {@code valueLoader} raises an exception,
   * it will be propagated to the returned {@code CompletableFuture} handle.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which this cache maps the specified key, contained
   * within a {@link CompletableFuture} which will never be {@code null}.
   * The provided future is expected to produce a value or raise an exception.
   * @see #retrieve(Object)
   * @see #get(Object, ThrowingFunction)
   */
  default <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    throw new UnsupportedOperationException(
            getClass().getName() + " does not support CompletableFuture-based retrieval");
  }

  /**
   * Associate the specified value with the specified key in this cache.
   * <p>If the cache previously contained a mapping for this key, the old
   * value is replaced by the specified value.
   * <p>Actual registration may be performed in an asynchronous or deferred
   * fashion, with subsequent lookups possibly not seeing the entry yet.
   * This may for example be the case with transactional cache decorators.
   * Use {@link #putIfAbsent} for guaranteed immediate registration.
   *
   * @param key the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   * @see #putIfAbsent(Object, Object)
   */
  void put(Object key, @Nullable Object value);

  /**
   * Atomically associate the specified value with the specified key in this cache
   * if it is not set already.
   * <p>This is equivalent to:
   * <pre><code>
   * ValueWrapper existingValue = cache.get(key);
   * if (existingValue == null) {
   *     cache.put(key, value);
   * }
   * return existingValue;
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
  default ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    ValueWrapper existingValue = get(key);
    if (existingValue == null) {
      put(key, value);
    }
    return existingValue;
  }

  /**
   * Evict the mapping for this key from this cache if it is present.
   * <p>Actual eviction may be performed in an asynchronous or deferred
   * fashion, with subsequent lookups possibly still seeing the entry.
   * This may for example be the case with transactional cache decorators.
   * Use {@link #evictIfPresent} for guaranteed immediate removal.
   *
   * @param key the key whose mapping is to be removed from the cache
   * @see #evictIfPresent(Object)
   */
  void evict(Object key);

  /**
   * Evict the mapping for this key from this cache if it is present,
   * expecting the key to be immediately invisible for subsequent lookups.
   * <p>The default implementation delegates to {@link #evict(Object)},
   * returning {@code false} for not-determined prior presence of the key.
   * Cache providers and in particular cache decorators are encouraged
   * to perform immediate eviction if possible (e.g. in case of generally
   * deferred cache operations within a transaction) and to reliably
   * determine prior presence of the given key.
   *
   * @param key the key whose mapping is to be removed from the cache
   * @return {@code true} if the cache was known to have a mapping for
   * this key before, {@code false} if it did not (or if prior presence
   * could not be determined)
   * @see #evict(Object)
   * @since 4.0
   */
  default boolean evictIfPresent(Object key) {
    evict(key);
    return false;
  }

  /**
   * Clear the cache through removing all mappings.
   * <p>Actual clearing may be performed in an asynchronous or deferred
   * fashion, with subsequent lookups possibly still seeing the entries.
   * This may for example be the case with transactional cache decorators.
   * Use {@link #invalidate()} for guaranteed immediate removal of entries.
   *
   * @see #invalidate()
   */
  void clear();

  /**
   * Invalidate the cache through removing all mappings, expecting all
   * entries to be immediately invisible for subsequent lookups.
   *
   * @return {@code true} if the cache was known to have mappings before,
   * {@code false} if it did not (or if prior presence of entries could
   * not be determined)
   * @see #clear()
   * @since 4.0
   */
  default boolean invalidate() {
    clear();
    return false;
  }

  /**
   * A (wrapper) object representing a cache value.
   */
  @FunctionalInterface
  interface ValueWrapper {

    /**
     * Return the actual value in the cache.
     */
    @Nullable
    Object get();
  }

  /**
   * Wrapper exception to be thrown from {@link #get(Object, ThrowingFunction)}
   * in case of the value loader callback failing with an exception.
   */
  @SuppressWarnings("serial")
  class ValueRetrievalException extends RuntimeException {

    @Nullable
    private final Object key;

    public ValueRetrievalException(@Nullable Object key, Object loader, Throwable ex) {
      super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
      this.key = key;
    }

    @Nullable
    public Object getKey() {
      return this.key;
    }
  }

}
