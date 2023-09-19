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

package cn.taketoday.cache.jcache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import cn.taketoday.context.testfixture.cache.AbstractValueAdaptingCacheTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class JCacheEhCacheApiTests extends AbstractValueAdaptingCacheTests<JCacheCache> {

  private CacheManager cacheManager;

  private Cache<Object, Object> nativeCache;

  private JCacheCache cache;

  private JCacheCache cacheNoNull;

  @BeforeEach
  public void setup() {
    this.cacheManager = getCachingProvider().getCacheManager();
    this.cacheManager.createCache(CACHE_NAME, new MutableConfiguration<>());
    this.cacheManager.createCache(CACHE_NAME_NO_NULL, new MutableConfiguration<>());
    this.nativeCache = this.cacheManager.getCache(CACHE_NAME);
    this.cache = new JCacheCache(this.nativeCache);
    Cache<Object, Object> nativeCacheNoNull =
            this.cacheManager.getCache(CACHE_NAME_NO_NULL);
    this.cacheNoNull = new JCacheCache(nativeCacheNoNull, false);
  }

  protected CachingProvider getCachingProvider() {
    return Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
  }

  @AfterEach
  public void shutdown() {
    if (this.cacheManager != null) {
      this.cacheManager.close();
    }
  }

  @Override
  protected JCacheCache getCache() {
    return getCache(true);
  }

  @Override
  protected JCacheCache getCache(boolean allowNull) {
    return (allowNull ? this.cache : this.cacheNoNull);
  }

  @Override
  protected Object getNativeCache() {
    return this.nativeCache;
  }

  @Test
  void testPutIfAbsentNullValue() {
    JCacheCache cache = getCache(true);

    String key = createRandomKey();
    String value = null;

    assertThat(cache.get(key)).isNull();
    assertThat(cache.putIfAbsent(key, value)).isNull();
    assertThat(cache.get(key).get()).isEqualTo(value);
    cn.taketoday.cache.Cache.ValueWrapper wrapper = cache.putIfAbsent(key, "anotherValue");
    // A value is set but is 'null'
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.get()).isNull();
    // not changed
    assertThat(cache.get(key).get()).isEqualTo(value);
  }

}
