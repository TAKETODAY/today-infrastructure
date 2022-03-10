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

import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheManager;

import cn.taketoday.cache.transaction.AbstractTransactionSupportingCacheManagerTests;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
public class JCacheCacheManagerTests extends AbstractTransactionSupportingCacheManagerTests<JCacheCacheManager> {

  private CacheManagerMock cacheManagerMock;

  private JCacheCacheManager cacheManager;

  private JCacheCacheManager transactionalCacheManager;

  @BeforeEach
  public void setupOnce() {
    cacheManagerMock = new CacheManagerMock();
    cacheManagerMock.addCache(CACHE_NAME);

    cacheManager = new JCacheCacheManager(cacheManagerMock.getCacheManager());
    cacheManager.setTransactionAware(false);
    cacheManager.afterPropertiesSet();

    transactionalCacheManager = new JCacheCacheManager(cacheManagerMock.getCacheManager());
    transactionalCacheManager.setTransactionAware(true);
    transactionalCacheManager.afterPropertiesSet();
  }

  @Override
  protected JCacheCacheManager getCacheManager(boolean transactionAware) {
    if (transactionAware) {
      return transactionalCacheManager;
    }
    else {
      return cacheManager;
    }
  }

  @Override
  protected Class<? extends cn.taketoday.cache.Cache> getCacheType() {
    return JCacheCache.class;
  }

  @Override
  protected void addNativeCache(String cacheName) {
    cacheManagerMock.addCache(cacheName);
  }

  @Override
  protected void removeNativeCache(String cacheName) {
    cacheManagerMock.removeCache(cacheName);
  }

  private static class CacheManagerMock {

    private final List<String> cacheNames;

    private final CacheManager cacheManager;

    private CacheManagerMock() {
      this.cacheNames = new ArrayList<>();
      this.cacheManager = mock(CacheManager.class);
      given(cacheManager.getCacheNames()).willReturn(cacheNames);
    }

    private CacheManager getCacheManager() {
      return cacheManager;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addCache(String name) {
      cacheNames.add(name);
      Cache cache = mock(Cache.class);
      given(cache.getName()).willReturn(name);
      given(cacheManager.getCache(name)).willReturn(cache);
    }

    public void removeCache(String name) {
      cacheNames.remove(name);
      given(cacheManager.getCache(name)).willReturn(null);
    }
  }

}
