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

import java.util.concurrent.Callable;

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * Cache decorator which synchronizes its {@link #put}, {@link #evict} and
 * {@link #clear} operations with Framework-managed transactions (through Framework's
 * {@link TransactionSynchronizationManager}, performing the actual cache
 * put/evict/clear operation only in the after-commit phase of a successful
 * transaction. If no transaction is active, {@link #put}, {@link #evict} and
 * {@link #clear} operations will be performed immediately, as usual.
 *
 * <p><b>Note:</b> Use of immediate operations such as {@link #putIfAbsent} and
 * {@link #evictIfPresent} cannot be deferred to the after-commit phase of a
 * running transaction. Use these with care in a transactional environment.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Stas Volsky
 * @see TransactionAwareCacheManagerProxy
 * @since 4.0
 */
public class TransactionAwareCacheDecorator implements Cache {

  private final Cache targetCache;

  /**
   * Create a new TransactionAwareCache for the given target Cache.
   *
   * @param targetCache the target Cache to decorate
   */
  public TransactionAwareCacheDecorator(Cache targetCache) {
    Assert.notNull(targetCache, "Target Cache must not be null");
    this.targetCache = targetCache;
  }

  /**
   * Return the target Cache that this Cache should delegate to.
   */
  public Cache getTargetCache() {
    return this.targetCache;
  }

  @Override
  public String getName() {
    return this.targetCache.getName();
  }

  @Override
  public Object getNativeCache() {
    return this.targetCache.getNativeCache();
  }

  @Override
  @Nullable
  public ValueWrapper get(Object key) {
    return this.targetCache.get(key);
  }

  @Override
  public <T> T get(Object key, @Nullable Class<T> type) {
    return this.targetCache.get(key, type);
  }

  @Override
  @Nullable
  public <T> T get(Object key, Callable<T> valueLoader) {
    return this.targetCache.get(key, valueLoader);
  }

  @Override
  public void put(final Object key, @Nullable final Object value) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          TransactionAwareCacheDecorator.this.targetCache.put(key, value);
        }
      });
    }
    else {
      this.targetCache.put(key, value);
    }
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    return this.targetCache.putIfAbsent(key, value);
  }

  @Override
  public void evict(final Object key) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          TransactionAwareCacheDecorator.this.targetCache.evict(key);
        }
      });
    }
    else {
      this.targetCache.evict(key);
    }
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return this.targetCache.evictIfPresent(key);
  }

  @Override
  public void clear() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          targetCache.clear();
        }
      });
    }
    else {
      this.targetCache.clear();
    }
  }

  @Override
  public boolean invalidate() {
    return this.targetCache.invalidate();
  }

}
