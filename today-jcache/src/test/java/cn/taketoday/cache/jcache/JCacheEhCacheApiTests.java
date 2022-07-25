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

package cn.taketoday.cache.jcache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import cn.taketoday.cache.jcache.testfixture.cache.AbstractCacheTests;
import cn.taketoday.cache.jcache.testfixture.cache.AbstractValueAdaptingCacheTests;

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
    this.cacheManager.createCache(AbstractCacheTests.CACHE_NAME, new MutableConfiguration<>());
    this.cacheManager.createCache(AbstractValueAdaptingCacheTests.CACHE_NAME_NO_NULL, new MutableConfiguration<>());
    this.nativeCache = this.cacheManager.getCache(AbstractCacheTests.CACHE_NAME);
    this.cache = new JCacheCache(this.nativeCache);
    Cache<Object, Object> nativeCacheNoNull =
            this.cacheManager.getCache(AbstractValueAdaptingCacheTests.CACHE_NAME_NO_NULL);
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
    return allowNull ? this.cache : this.cacheNoNull;
  }

  @Override
  protected Object getNativeCache() {
    return this.nativeCache;
  }

}
