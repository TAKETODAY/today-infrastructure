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

package infra.web.resource;

import java.io.IOException;

import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.RequestContext;

/**
 * A {@link infra.web.resource.ResourceTransformer} that checks a
 * {@link Cache} to see if a previously transformed resource
 * exists in the cache and returns it if found, and otherwise delegates to the resolver
 * chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CachingResourceTransformer implements ResourceTransformer {

  private static final Logger logger = LoggerFactory.getLogger(CachingResourceTransformer.class);

  private final Cache cache;

  public CachingResourceTransformer(Cache cache) {
    Assert.notNull(cache, "Cache is required");
    this.cache = cache;
  }

  public CachingResourceTransformer(CacheManager cacheManager, String cacheName) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      throw new IllegalArgumentException("Cache '%s' not found".formatted(cacheName));
    }
    this.cache = cache;
  }

  /**
   * Return the configured {@code Cache}.
   */
  public Cache getCache() {
    return this.cache;
  }

  @Override
  public Resource transform(RequestContext request, Resource resource, ResourceTransformerChain transformerChain)
          throws IOException {

    Resource transformed = this.cache.get(resource, Resource.class);
    if (transformed != null) {
      logger.trace("Resource resolved from cache");
      return transformed;
    }

    transformed = transformerChain.transform(request, resource);
    this.cache.put(resource, transformed);

    return transformed;
  }

}
