/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.cache.support;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.LoadingCache;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.cache.Cache;
import infra.lang.Assert;
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
      result = result.thenApply(this::toValueWrapper);
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
