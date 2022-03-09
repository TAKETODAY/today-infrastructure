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

package cn.taketoday.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.Cache.ValueWrapper;
import cn.taketoday.cache.support.CaffeineCache;
import cn.taketoday.contextsupport.testfixture.cache.AbstractValueAdaptingCacheTests;

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

}
