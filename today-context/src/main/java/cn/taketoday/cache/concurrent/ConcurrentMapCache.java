/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.cache.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.support.AbstractValueAdaptingCache;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.core.serializer.support.SerializationDelegate;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple {@link Cache} implementation based on the
 * core JDK {@code java.util.concurrent} package.
 *
 * <p>Useful for testing or simple caching scenarios, typically in combination
 * with {@link SimpleCacheManager} or
 * dynamically through {@link ConcurrentMapCacheManager}.
 *
 * <p>Supports the  {@link #retrieve(Object)} and {@link #retrieve(Object, Supplier)}
 * operations in a best-effort fashion, relying on default {@link CompletableFuture}
 * execution (typically within the JVM's {@link ForkJoinPool#commonPool()}).
 *
 * <p><b>Note:</b> As {@link ConcurrentHashMap} (the default implementation used)
 * does not allow for {@code null} values to be stored, this class will replace
 * them with a predefined internal object. This behavior can be changed through the
 * {@link #ConcurrentMapCache(String, ConcurrentMap, boolean)} constructor.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConcurrentMapCacheManager
 * @since 2019-12-17 12:29
 */
public class ConcurrentMapCache extends AbstractValueAdaptingCache {

  private final String name;

  private final ConcurrentMap<Object, Object> store;

  @Nullable
  private final SerializationDelegate serialization;

  /**
   * Create a new ConcurrentMapCache with the specified name.
   *
   * @param name the name of the cache
   */
  public ConcurrentMapCache(String name) {
    this(name, new ConcurrentHashMap<>(256), true);
  }

  /**
   * Create a new ConcurrentMapCache with the specified name.
   *
   * @param name the name of the cache
   * @param allowNullValues whether to accept and convert {@code null}
   * values for this cache
   */
  public ConcurrentMapCache(String name, boolean allowNullValues) {
    this(name, new ConcurrentHashMap<>(256), allowNullValues);
  }

  /**
   * Create a new ConcurrentMapCache with the specified name and the
   * given internal {@link ConcurrentMap} to use.
   *
   * @param name the name of the cache
   * @param store the ConcurrentMap to use as an internal store
   * @param allowNullValues whether to allow {@code null} values
   * (adapting them to an internal null holder value)
   */
  public ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store, boolean allowNullValues) {
    this(name, store, allowNullValues, null);
  }

  /**
   * Create a new ConcurrentMapCache with the specified name and the
   * given internal {@link ConcurrentMap} to use. If the
   * {@link SerializationDelegate} is specified,
   * {@link #isStoreByValue() store-by-value} is enabled
   *
   * @param name the name of the cache
   * @param store the ConcurrentMap to use as an internal store
   * @param allowNullValues whether to allow {@code null} values
   * (adapting them to an internal null holder value)
   * @param serialization the {@link SerializationDelegate} to use
   * to serialize cache entry or {@code null} to store the reference
   * @since 4.0
   */
  protected ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store,
          boolean allowNullValues, @Nullable SerializationDelegate serialization) {

    super(allowNullValues);
    Assert.notNull(name, "Name is required");
    Assert.notNull(store, "Store is required");
    this.name = name;
    this.store = store;
    this.serialization = serialization;
  }

  /**
   * Return whether this cache stores a copy of each entry ({@code true}) or
   * a reference ({@code false}, default). If store by value is enabled, each
   * entry in the cache must be serializable.
   *
   * @since 4.0
   */
  public final boolean isStoreByValue() {
    return (this.serialization != null);
  }

  @Override
  public final String getName() {
    return this.name;
  }

  @Override
  public final ConcurrentMap<Object, Object> getNativeCache() {
    return this.store;
  }

  @Override
  @Nullable
  protected Object lookup(Object key) {
    return this.store.get(key);
  }

  @SuppressWarnings("unchecked")
  @Override
  @Nullable
  public <T> T get(Object key, Callable<T> valueLoader) {
    return (T) fromStoreValue(this.store.computeIfAbsent(key, k -> {
      try {
        return toStoreValue(valueLoader.call());
      }
      catch (Throwable ex) {
        throw new ValueRetrievalException(key, valueLoader, ex);
      }
    }));
  }

  @Override
  @Nullable
  public CompletableFuture<?> retrieve(Object key) {
    Object value = lookup(key);
    return value != null ? CompletableFuture.completedFuture(
            allowNullValues ? toValueWrapper(value) : fromStoreValue(value)) : null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    return CompletableFuture.supplyAsync(() ->
            (T) fromStoreValue(this.store.computeIfAbsent(key, k -> toStoreValue(valueLoader.get().join()))));
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    this.store.put(key, toStoreValue(value));
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    Object existing = this.store.putIfAbsent(key, toStoreValue(value));
    return toValueWrapper(existing);
  }

  @Override
  public void evict(Object key) {
    this.store.remove(key);
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return (this.store.remove(key) != null);
  }

  @Override
  public void clear() {
    this.store.clear();
  }

  @Override
  public boolean invalidate() {
    boolean notEmpty = !this.store.isEmpty();
    this.store.clear();
    return notEmpty;
  }

  @Override
  protected Object toStoreValue(@Nullable Object userValue) {
    Object storeValue = super.toStoreValue(userValue);
    if (this.serialization != null) {
      try {
        return this.serialization.serializeToByteArray(storeValue);
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException("Failed to serialize cache value '" + userValue +
                "'. Does it implement Serializable?", ex);
      }
    }
    else {
      return storeValue;
    }
  }

  @Override
  protected Object fromStoreValue(@Nullable Object storeValue) {
    if (storeValue != null && this.serialization != null) {
      try {
        return super.fromStoreValue(this.serialization.deserializeFromByteArray((byte[]) storeValue));
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException("Failed to deserialize cache value '" + storeValue + "'", ex);
      }
    }
    else {
      return super.fromStoreValue(storeValue);
    }
  }

}
