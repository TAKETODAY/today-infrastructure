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

import infra.cache.Cache;
import infra.cache.support.AbstractCacheManager;

/**
 * Base class for CacheManager implementations that want to support built-in
 * awareness of Framework-managed transactions. This usually needs to be switched
 * on explicitly through the {@link #setTransactionAware} bean property.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #setTransactionAware
 * @see TransactionAwareCacheDecorator
 * @see TransactionAwareCacheManagerProxy
 * @since 4.0
 */
public abstract class AbstractTransactionSupportingCacheManager extends AbstractCacheManager {

  private boolean transactionAware = false;

  /**
   * Set whether this CacheManager should expose transaction-aware Cache objects.
   * <p>Default is "false". Set this to "true" to synchronize cache put/evict
   * operations with ongoing Framework-managed transactions, performing the actual cache
   * put/evict operation only in the after-commit phase of a successful transaction.
   */
  public void setTransactionAware(boolean transactionAware) {
    this.transactionAware = transactionAware;
  }

  /**
   * Return whether this CacheManager has been configured to be transaction-aware.
   */
  public boolean isTransactionAware() {
    return this.transactionAware;
  }

  @Override
  protected Cache decorateCache(Cache cache) {
    return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache);
  }

}
