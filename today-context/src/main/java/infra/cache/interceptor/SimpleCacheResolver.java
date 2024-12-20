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

package infra.cache.interceptor;

import java.util.Collection;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.lang.Nullable;

/**
 * A simple {@link CacheResolver} that resolves the {@link Cache} instance(s)
 * based on a configurable {@link CacheManager} and the name of the
 * cache(s) as provided by {@link BasicOperation#getCacheNames() getCacheNames()}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BasicOperation#getCacheNames()
 * @since 4.0
 */
public class SimpleCacheResolver extends AbstractCacheResolver {

  /**
   * Construct a new {@code SimpleCacheResolver}.
   *
   * @see #setCacheManager
   */
  public SimpleCacheResolver() { }

  /**
   * Construct a new {@code SimpleCacheResolver} for the given {@link CacheManager}.
   *
   * @param cacheManager the CacheManager to use
   */
  public SimpleCacheResolver(CacheManager cacheManager) {
    super(cacheManager);
  }

  @Override
  protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
    return context.getOperation().getCacheNames();
  }

  /**
   * Return a {@code SimpleCacheResolver} for the given {@link CacheManager}.
   *
   * @param cacheManager the CacheManager (potentially {@code null})
   * @return the SimpleCacheResolver ({@code null} if the CacheManager was {@code null})
   */
  @Nullable
  static SimpleCacheResolver of(@Nullable CacheManager cacheManager) {
    return (cacheManager != null ? new SimpleCacheResolver(cacheManager) : null);
  }

}
