/*
 * Copyright 2017 - 2024 the original author or authors.
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
package infra.cache.support;

import java.util.Collection;
import java.util.Collections;

import infra.cache.Cache;

/**
 * Simple cache manager working against a given collection of caches.
 * Useful for testing or simple caching declarations.
 * <p>
 * When using this implementation directly, i.e. not via a regular
 * bean registration, {@link #initializeCaches()} should be invoked
 * to initialize its internal state once the
 * {@linkplain #setCaches(Collection) caches have been provided}.
 *
 * @author Costin Leau
 * @author TODAY
 * @since 2019-11-03 19:45
 */
public class SimpleCacheManager extends AbstractCacheManager {

  private Collection<? extends Cache> caches = Collections.emptySet();

  /**
   * Specify the collection of Cache instances to use for this CacheManager.
   *
   * @see #initializeCaches()
   */
  public void setCaches(Collection<? extends Cache> caches) {
    this.caches = caches;
  }

  @Override
  protected Collection<? extends Cache> loadCaches() {
    return this.caches;
  }

}
