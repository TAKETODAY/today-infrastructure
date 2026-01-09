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

package infra.cache.transaction;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

import infra.beans.factory.InitializingBean;
import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.lang.Assert;

/**
 * Proxy for a target {@link CacheManager}, exposing transaction-aware {@link Cache} objects
 * which synchronize their {@link Cache#put} operations with Framework-managed transactions
 * (through Framework's {@link infra.transaction.support.TransactionSynchronizationManager},
 * performing the actual cache put operation only in the after-commit phase of a successful transaction.
 * If no transaction is active, {@link Cache#put} operations will be performed immediately, as usual.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #setTargetCacheManager
 * @see TransactionAwareCacheDecorator
 * @see infra.transaction.support.TransactionSynchronizationManager
 * @since 4.0
 */
public class TransactionAwareCacheManagerProxy implements CacheManager, InitializingBean {

  private @Nullable CacheManager targetCacheManager;

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
    setTargetCacheManager(targetCacheManager);
  }

  /**
   * Set the target CacheManager to proxy.
   */
  public void setTargetCacheManager(CacheManager targetCacheManager) {
    Assert.notNull(targetCacheManager, "Target CacheManager is required");
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
