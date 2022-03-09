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

package cn.taketoday.cache.transaction;

import java.util.Collection;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Proxy for a target {@link CacheManager}, exposing transaction-aware {@link Cache} objects
 * which synchronize their {@link Cache#put} operations with Spring-managed transactions
 * (through Framework's {@link cn.taketoday.transaction.support.TransactionSynchronizationManager},
 * performing the actual cache put operation only in the after-commit phase of a successful transaction.
 * If no transaction is active, {@link Cache#put} operations will be performed immediately, as usual.
 *
 * @author Juergen Hoeller
 * @see #setTargetCacheManager
 * @see TransactionAwareCacheDecorator
 * @see cn.taketoday.transaction.support.TransactionSynchronizationManager
 * @since 4.0
 */
public class TransactionAwareCacheManagerProxy implements CacheManager, InitializingBean {

  @Nullable
  private CacheManager targetCacheManager;

  /**
   * Create a new TransactionAwareCacheManagerProxy, setting the target CacheManager
   * through the {@link #setTargetCacheManager} bean property.
   */
  public TransactionAwareCacheManagerProxy() {
  }

  /**
   * Create a new TransactionAwareCacheManagerProxy for the given target CacheManager.
   *
   * @param targetCacheManager the target CacheManager to proxy
   */
  public TransactionAwareCacheManagerProxy(CacheManager targetCacheManager) {
    Assert.notNull(targetCacheManager, "Target CacheManager must not be null");
    this.targetCacheManager = targetCacheManager;
  }

  /**
   * Set the target CacheManager to proxy.
   */
  public void setTargetCacheManager(CacheManager targetCacheManager) {
    this.targetCacheManager = targetCacheManager;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.targetCacheManager == null) {
      throw new IllegalArgumentException("Property 'targetCacheManager' is required");
    }
  }

  @Override
  @Nullable
  public Cache getCache(String name) {
    Assert.state(this.targetCacheManager != null, "No target CacheManager set");
    Cache targetCache = this.targetCacheManager.getCache(name);
    return (targetCache != null ? new TransactionAwareCacheDecorator(targetCache) : null);
  }

  @Override
  public Collection<String> getCacheNames() {
    Assert.state(this.targetCacheManager != null, "No target CacheManager set");
    return this.targetCacheManager.getCacheNames();
  }

}
