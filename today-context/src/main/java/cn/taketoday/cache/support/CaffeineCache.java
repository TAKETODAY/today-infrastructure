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

package cn.taketoday.cache.support;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Infra {@link Cache} adapter implementation
 * on top of a Caffeine {@link com.github.benmanes.caffeine.cache.Cache} instance.
 *
 * <p>Supports the {@link #retrieve(Object)} and {@link #retrieve(Object, Supplier)}
 * operations through Caffeine's {@link AsyncCache}, when provided via the
 * {@link #CaffeineCache(String, AsyncCache, boolean)} constructor.
 *
 * <p>Requires Caffeine 3.0 or higher, as of Spring Framework 6.1.
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author TODAY 2020-08-15 19:50
 * @see CaffeineCacheManager
 * @since 3.0
 */
public class CaffeineCache extends AbstractValueAdaptingCache {

  private final String name;

  private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;

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
  public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
          boolean allowNullValues) {

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
    return this.name;
  }

  /**
   * Return the internal Caffeine Cache
   * (possibly an adapter on top of an {@link #getAsyncCache()}).
   */
  @Override
  public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
    return this.cache;
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
    Assert.state(this.asyncCache != null,
            "No Caffeine AsyncCache available: set CaffeineCacheManager.setAsyncCacheMode(true)");
    return this.asyncCache;
  }

  @SuppressWarnings("unchecked")
  @Override
  @Nullable
  public <T> T get(Object key, final Callable<T> valueLoader) {
    return (T) fromStoreValue(this.cache.get(key, new LoadFunction(valueLoader)));
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

  @SuppressWarnings("unchecked")
  @Override
  public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    return (CompletableFuture<T>) getAsyncCache().get(key, (k, e) -> valueLoader.get());
  }

  @Override
  @Nullable
  protected Object lookup(Object key) {
    if (this.cache instanceof LoadingCache) {
      return ((LoadingCache<Object, Object>) this.cache).get(key);
    }
    return this.cache.getIfPresent(key);
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    this.cache.put(key, toStoreValue(value));
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
    PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
    Object result = this.cache.get(key, callable);
    return (callable.called ? null : toValueWrapper(result));
  }

  @Override
  public void evict(Object key) {
    this.cache.invalidate(key);
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return (this.cache.asMap().remove(key) != null);
  }

  @Override
  public void clear() {
    this.cache.invalidateAll();
  }

  @Override
  public boolean invalidate() {
    boolean notEmpty = !this.cache.asMap().isEmpty();
    this.cache.invalidateAll();
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
      return toStoreValue(this.value);
    }
  }

  private class LoadFunction implements Function<Object, Object> {

    private final Callable<?> valueLoader;

    public LoadFunction(Callable<?> valueLoader) {
      this.valueLoader = valueLoader;
    }

    @Override
    public Object apply(Object o) {
      try {
        return toStoreValue(this.valueLoader.call());
      }
      catch (Exception ex) {
        throw new ValueRetrievalException(o, this.valueLoader, ex);
      }
    }
  }

}
