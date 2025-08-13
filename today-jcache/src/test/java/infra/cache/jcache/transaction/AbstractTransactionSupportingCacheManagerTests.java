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

package infra.cache.jcache.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.transaction.AbstractTransactionSupportingCacheManager;
import infra.cache.transaction.TransactionAwareCacheDecorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared tests for {@link CacheManager} that inherit from
 * {@link AbstractTransactionSupportingCacheManager}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public abstract class AbstractTransactionSupportingCacheManagerTests<T extends CacheManager> {

  public static final String CACHE_NAME = "testCacheManager";

  protected String cacheName;

  @BeforeEach
  void trackCacheName(TestInfo testInfo) {
    this.cacheName = testInfo.getTestMethod().get().getName();
  }

  /**
   * Returns the {@link CacheManager} to use.
   *
   * @param transactionAware if the requested cache manager should be aware
   * of the transaction
   * @return the cache manager to use
   * @see infra.cache.transaction.AbstractTransactionSupportingCacheManager#setTransactionAware
   */
  protected abstract T getCacheManager(boolean transactionAware);

  /**
   * Returns the expected concrete type of the cache.
   */
  protected abstract Class<? extends Cache> getCacheType();

  /**
   * Adds a cache with the specified name to the native manager.
   */
  protected abstract void addNativeCache(String cacheName);

  /**
   * Removes the cache with the specified name from the native manager.
   */
  protected abstract void removeNativeCache(String cacheName);

  @Test
  public void getOnExistingCache() {
    assertThat(getCacheManager(false).getCache(CACHE_NAME)).isInstanceOf(getCacheType());
  }

  @Test
  public void getOnNewCache() {
    T cacheManager = getCacheManager(false);
    addNativeCache(this.cacheName);
    assertThat(cacheManager.getCacheNames().contains(this.cacheName)).isFalse();
    try {
      assertThat(cacheManager.getCache(this.cacheName)).isInstanceOf(getCacheType());
      assertThat(cacheManager.getCacheNames().contains(this.cacheName)).isTrue();
    }
    finally {
      removeNativeCache(this.cacheName);
    }
  }

  @Test
  public void getOnUnknownCache() {
    T cacheManager = getCacheManager(false);
    assertThat(cacheManager.getCacheNames().contains(this.cacheName)).isFalse();
    assertThat(cacheManager.getCache(this.cacheName)).isNull();
  }

  @Test
  public void getTransactionalOnExistingCache() {
    assertThat(getCacheManager(true).getCache(CACHE_NAME))
            .isInstanceOf(TransactionAwareCacheDecorator.class);
  }

  @Test
  public void getTransactionalOnNewCache() {
    T cacheManager = getCacheManager(true);
    assertThat(cacheManager.getCacheNames().contains(this.cacheName)).isFalse();
    addNativeCache(this.cacheName);
    try {
      assertThat(cacheManager.getCache(this.cacheName))
              .isInstanceOf(TransactionAwareCacheDecorator.class);
      assertThat(cacheManager.getCacheNames().contains(this.cacheName)).isTrue();
    }
    finally {
      removeNativeCache(this.cacheName);
    }
  }

}
