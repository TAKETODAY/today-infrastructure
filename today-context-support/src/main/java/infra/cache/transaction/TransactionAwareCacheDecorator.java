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

package infra.cache.transaction;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import infra.cache.Cache;
import infra.lang.Assert;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;
import infra.util.function.ThrowingFunction;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
    Assert.notNull(targetCache, "Target Cache is required");
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

  @Nullable
  @Override
  public <T> T get(Object key, @Nullable Class<T> type) {
    return this.targetCache.get(key, type);
  }

  @Nullable
  @Override
  public <K, V> V get(K key, ThrowingFunction<? super K, ? extends V> valueLoader) {
    return this.targetCache.get(key, valueLoader);
  }

  @Override
  @Nullable
  public CompletableFuture<?> retrieve(Object key) {
    return this.targetCache.retrieve(key);
  }

  @Override
  public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    return this.targetCache.retrieve(key, valueLoader);
  }

  @Override
  public void put(final Object key, @Nullable final Object value) {
    var info = TransactionSynchronizationManager.getSynchronizationInfo();
    if (info.isSynchronizationActive()) {
      info.registerSynchronization(new TransactionSynchronization() {
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
    var info = TransactionSynchronizationManager.getSynchronizationInfo();
    if (info.isSynchronizationActive()) {
      info.registerSynchronization(new TransactionSynchronization() {
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
    var info = TransactionSynchronizationManager.getSynchronizationInfo();
    if (info.isSynchronizationActive()) {
      info.registerSynchronization(new TransactionSynchronization() {
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
