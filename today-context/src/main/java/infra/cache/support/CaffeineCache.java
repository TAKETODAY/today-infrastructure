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

package infra.cache.support;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.cache.Cache;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.function.ThrowingFunction;

/**
 * Infra {@link Cache} adapter implementation
 * on top of a Caffeine {@link com.github.benmanes.caffeine.cache.Cache} instance.
 *
 * <p>Supports the {@link #retrieve(Object)} and {@link #retrieve(Object, Supplier)}
 * operations through Caffeine's {@link AsyncCache}, when provided via the
 * {@link #CaffeineCache(String, AsyncCache, boolean)} constructor.
 *
 * <p>Requires Caffeine 3.0 or higher.
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see CaffeineCacheManager
 * @since 3.0 2020-08-15 19:50
 */
@SuppressWarnings("unchecked")
public class CaffeineCache extends AbstractValueAdaptingCache {

  private final String name;

  @SuppressWarnings("rawtypes")
  private final com.github.benmanes.caffeine.cache.Cache cache;

  @Nullable
  private AsyncCache<Object, Object> asyncCache;

  /**
   * Create a {@link CaffeineCache} instance with the specified name and the
   * given internal {@link com.github.benmanes.caffeine.cache.Cache} to use.
   *
   * @param name the name of the cache
   * @param cache the backing Caffeine Cache instance
   */
  public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
    this(name, cache, true);
  }

  /**
   * Create a {@link CaffeineCache} instance with the specified name and the
   * given internal {@link com.github.benmanes.caffeine.cache.Cache} to use.
   *
   * @param name the name of the cache
   * @param cache the backing Caffeine Cache instance
   * @param allowNullValues whether to accept and convert {@code null}
   * values for this cache
   */
  public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache, boolean allowNullValues) {
    super(allowNullValues);
    Assert.notNull(name, "Name is required");
    Assert.notNull(cache, "Cache is required");
    this.name = name;
    this.cache = cache;
  }

  /**
   * Create a {@link CaffeineCache} instance with the specified name and the
   * given internal {@link AsyncCache} to use.
   *
   * @param name the name of the cache
   * @param cache the backing Caffeine AsyncCache instance
   * @param allowNullValues whether to accept and convert {@code null} values
   * for this cache
   */
  public CaffeineCache(String name, AsyncCache<Object, Object> cache, boolean allowNullValues) {
    super(allowNullValues);
    Assert.notNull(name, "Name is required");
    Assert.notNull(cache, "Cache is required");
    this.name = name;
    this.cache = cache.synchronous();
    this.asyncCache = cache;
  }

  @Override
  public final String getName() {
    return name;
  }

  /**
   * Return the internal Caffeine Cache
   * (possibly an adapter on top of an {@link #getAsyncCache()}).
   */
  @Override
  public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
    return cache;
  }

  /**
   * Return the internal Caffeine AsyncCache.
   *
   * @throws IllegalStateException if no AsyncCache is available
   * @see #CaffeineCache(String, AsyncCache, boolean)
   * @see CaffeineCacheManager#setAsyncCacheMode
   * @since 4.0
   */
  public final AsyncCache<Object, Object> getAsyncCache() {
    Assert.state(asyncCache != null,
            "No Caffeine AsyncCache available: set CaffeineCacheManager.setAsyncCacheMode(true)");
    return asyncCache;
  }

  @Nullable
  public <K, V> V get(K key, ThrowingFunction<? super K, ? extends V> valueLoader) {
    return (V) fromStoreValue(cache.get(key, new LoadFunction(valueLoader)));
  }

  @Override
  @Nullable
  public CompletableFuture<?> retrieve(Object key) {
    CompletableFuture<?> result = getAsyncCache().getIfPresent(key);
    if (result != null && allowNullValues) {
      result = result.handle((value, ex) -> fromStoreValue(value));
    }
    return result;
  }

  @Override
  public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    if (allowNullValues) {
      return (CompletableFuture<T>) getAsyncCache()
              .get(key, (k, e) -> valueLoader.get().thenApply(this::toStoreValue))
              .thenApply(this::fromStoreValue);
    }
    else {
      return (CompletableFuture<T>) getAsyncCache().get(key, (k, e) -> valueLoader.get());
    }
  }

  @Override
  @Nullable
  protected Object lookup(Object key) {
    if (cache instanceof LoadingCache) {
      return ((LoadingCache<Object, Object>) cache).get(key);
    }
    return cache.getIfPresent(key);
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    cache.put(key, toStoreValue(value));
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
    PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
    Object result = cache.get(key, callable);
    return (callable.called ? null : toValueWrapper(result));
  }

  @Override
  public void evict(Object key) {
    cache.invalidate(key);
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return (cache.asMap().remove(key) != null);
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

  @Override
  public boolean invalidate() {
    boolean notEmpty = !cache.asMap().isEmpty();
    cache.invalidateAll();
    return notEmpty;
  }

  private class PutIfAbsentFunction implements Function<Object, Object> {

    @Nullable
    private final Object value;

    private boolean called;

    public PutIfAbsentFunction(@Nullable Object value) {
      this.value = value;
    }

    @Override
    public Object apply(Object key) {
      this.called = true;
      return toStoreValue(value);
    }
  }

  private class LoadFunction implements Function<Object, Object> {

    private final ThrowingFunction<Object, Object> valueLoader;

    @SuppressWarnings("rawtypes")
    public LoadFunction(ThrowingFunction valueLoader) {
      this.valueLoader = valueLoader;
    }

    @Override
    public Object apply(Object o) {
      try {
        return toStoreValue(valueLoader.apply(o));
      }
      catch (Exception ex) {
        throw new ValueRetrievalException(o, valueLoader, ex);
      }
    }
  }

}
