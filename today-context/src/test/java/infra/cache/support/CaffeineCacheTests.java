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
import com.github.benmanes.caffeine.cache.Caffeine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import infra.cache.Cache;
import infra.cache.Cache.ValueWrapper;
import infra.context.testfixture.cache.AbstractValueAdaptingCacheTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CaffeineCache}.
 *
 * @author Ben Manes
 * @author Stephane Nicoll
 */
class CaffeineCacheTests extends AbstractValueAdaptingCacheTests<CaffeineCache> {

  private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache;

  private CaffeineCache cache;

  private CaffeineCache cacheNoNull;

  @BeforeEach
  void setUp() {
    nativeCache = Caffeine.newBuilder().build();
    cache = new CaffeineCache(CACHE_NAME, nativeCache);
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCacheNoNull
            = Caffeine.newBuilder().build();
    cacheNoNull = new CaffeineCache(CACHE_NAME_NO_NULL, nativeCacheNoNull, false);
  }

  @Override
  protected CaffeineCache getCache() {
    return getCache(true);
  }

  @Override
  protected CaffeineCache getCache(boolean allowNull) {
    return allowNull ? this.cache : this.cacheNoNull;
  }

  @Override
  protected Object getNativeCache() {
    return nativeCache;
  }

  @Test
  void testLoadingCacheGet() {
    Object value = new Object();
    CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
            .build(key -> value));
    ValueWrapper valueWrapper = loadingCache.get(new Object());
    assertThat(valueWrapper).isNotNull();
    assertThat(valueWrapper.get()).isEqualTo(value);
  }

  @Test
  void testLoadingCacheGetWithType() {
    String value = "value";
    CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
            .build(key -> value));
    String valueWrapper = loadingCache.get(new Object(), String.class);
    assertThat(valueWrapper).isNotNull();
    assertThat(valueWrapper).isEqualTo(value);
  }

  @Test
  void testLoadingCacheGetWithWrongType() {
    String value = "value";
    CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
            .build(key -> value));
    assertThatIllegalStateException().isThrownBy(() -> loadingCache.get(new Object(), Long.class));
  }

  @Test
  void testPutIfAbsentNullValue() {
    CaffeineCache cache = getCache();

    Object key = new Object();
    Object value = null;

    assertThat(cache.get(key)).isNull();
    assertThat(cache.putIfAbsent(key, value)).isNull();
    assertThat(cache.get(key).get()).isEqualTo(value);
    Cache.ValueWrapper wrapper = cache.putIfAbsent(key, "anotherValue");
    // A value is set but is 'null'
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.get()).isNull();
    // not changed
    assertThat(cache.get(key).get()).isEqualTo(value);
  }

  @Test
  void putIfAbsentWithExistingValue() {
    CaffeineCache cache = getCache();
    Object key = new Object();
    Object value = "initialValue";
    Object newValue = "newValue";

    cache.put(key, value);
    Cache.ValueWrapper wrapper = cache.putIfAbsent(key, newValue);

    assertThat(wrapper).isNotNull();
    assertThat(wrapper.get()).isEqualTo(value);
    assertThat(cache.get(key).get()).isEqualTo(value);
  }

  @Test
  void evictIfPresentReturnsTrueWhenKeyExists() {
    CaffeineCache cache = getCache();
    Object key = new Object();
    Object value = "value";

    cache.put(key, value);
    boolean result = cache.evictIfPresent(key);

    assertThat(result).isTrue();
    assertThat(cache.get(key)).isNull();
  }

  @Test
  void evictIfPresentReturnsFalseWhenKeyDoesNotExist() {
    CaffeineCache cache = getCache();
    Object key = new Object();

    boolean result = cache.evictIfPresent(key);

    assertThat(result).isFalse();
  }

  @Test
  void invalidateReturnsTrueWhenCacheNotEmpty() {
    CaffeineCache cache = getCache();
    Object key = new Object();
    Object value = "value";

    cache.put(key, value);
    boolean result = cache.invalidate();

    assertThat(result).isTrue();
    assertThat(cache.get(key)).isNull();
  }

  @Test
  void invalidateReturnsFalseWhenCacheIsEmpty() {
    CaffeineCache cache = getCache();

    boolean result = cache.invalidate();

    assertThat(result).isFalse();
  }

  @Test
  void retrieveWithAsyncCache() {
    AsyncCache<Object, Object> asyncCache = Caffeine.newBuilder().buildAsync();
    CaffeineCache cache = new CaffeineCache("test", asyncCache, true);

    Object key = new Object();
    Object value = "value";
    asyncCache.put(key, CompletableFuture.completedFuture(value));

    CompletableFuture<?> result = cache.retrieve(key);

    assertThat(result).isNotNull();
    assertThat(result.join()).isEqualTo(new SimpleValueWrapper(value));
  }

  @Test
  void retrieveWithAsyncCacheAndNullValuesNotAllowed() {
    AsyncCache<Object, Object> asyncCache = Caffeine.newBuilder().buildAsync();
    CaffeineCache cache = new CaffeineCache("test", asyncCache, false);

    Object key = new Object();
    Object value = "value";
    asyncCache.put(key, CompletableFuture.completedFuture(value));

    CompletableFuture<?> result = cache.retrieve(key);

    assertThat(result).isNotNull();
    assertThat(result.join()).isEqualTo(value);
  }

  @Test
  void getWithLoaderFunction() {
    CaffeineCache cache = getCache();
    Object key = "key";
    Object value = "value";

    Object result = cache.get(key, k -> value);

    assertThat(result).isEqualTo(value);
    assertThat(cache.get(key).get()).isEqualTo(value);
  }

  @Test
  void clearEmptiesTheCache() {
    CaffeineCache cache = getCache();
    Object key1 = new Object();
    Object value1 = "value1";
    Object key2 = new Object();
    Object value2 = "value2";

    cache.put(key1, value1);
    cache.put(key2, value2);
    cache.clear();

    assertThat(cache.get(key1)).isNull();
    assertThat(cache.get(key2)).isNull();
  }

  @Test
  void lookupReturnsValueFromLoadingCache() {
    Object value = "testValue";
    com.github.benmanes.caffeine.cache.Cache<Object, Object> loadingCache = Caffeine.newBuilder()
            .build(key -> value);
    CaffeineCache cache = new CaffeineCache("test", loadingCache);

    Object result = cache.lookup("key");

    assertThat(result).isEqualTo(value);
  }

  @Test
  void lookupReturnsNullForNonExistingKey() {
    CaffeineCache cache = getCache();
    Object result = cache.lookup("nonExistingKey");

    assertThat(result).isNull();
  }

  @Test
  void putStoresValueInCache() {
    CaffeineCache cache = getCache();
    Object key = "key";
    Object value = "value";

    cache.put(key, value);
    Object result = cache.get(key).get();

    assertThat(result).isEqualTo(value);
  }

  @Test
  void putWithNullValueWhenAllowed() {
    CaffeineCache cache = getCache();
    Object key = "key";
    Object value = null;

    cache.put(key, value);
    Object result = cache.get(key).get();

    assertThat(result).isNull();
  }

  @Test
  void evictRemovesKeyFromCache() {
    CaffeineCache cache = getCache();
    Object key = "key";
    Object value = "value";

    cache.put(key, value);
    cache.evict(key);
    Object result = cache.get(key);

    assertThat(result).isNull();
  }

  @Test
  void clearInvalidatesAllEntries() {
    CaffeineCache cache = getCache();
    Object key1 = "key1";
    Object value1 = "value1";
    Object key2 = "key2";
    Object value2 = "value2";

    cache.put(key1, value1);
    cache.put(key2, value2);
    cache.clear();

    assertThat(cache.get(key1)).isNull();
    assertThat(cache.get(key2)).isNull();
  }

  @Test
  void getAsyncCacheThrowsExceptionWhenNotAvailable() {
    com.github.benmanes.caffeine.cache.Cache<Object, Object> syncCache = Caffeine.newBuilder().build();
    CaffeineCache cache = new CaffeineCache("test", syncCache);

    assertThatIllegalStateException().isThrownBy(() -> cache.getAsyncCache())
            .withMessageContaining("No Caffeine AsyncCache available");
  }

  @Test
  void getAsyncCacheReturnsAsyncCacheWhenAvailable() {
    AsyncCache<Object, Object> asyncCache = Caffeine.newBuilder().buildAsync();
    CaffeineCache cache = new CaffeineCache("test", asyncCache, true);

    AsyncCache<Object, Object> result = cache.getAsyncCache();

    assertThat(result).isNotNull();
    assertThat(result).isSameAs(asyncCache);
  }

  @Test
  void retrieveReturnsNullWhenKeyNotPresent() {
    AsyncCache<Object, Object> asyncCache = Caffeine.newBuilder().buildAsync();
    CaffeineCache cache = new CaffeineCache("test", asyncCache, true);

    CompletableFuture<?> result = cache.retrieve("nonExistingKey");

    assertThat(result).isNull();
  }

}
