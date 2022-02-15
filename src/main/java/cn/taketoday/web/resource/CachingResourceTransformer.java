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

package cn.taketoday.web.resource;

import java.io.IOException;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * A {@link cn.taketoday.web.resource.ResourceTransformer} that checks a
 * {@link cn.taketoday.cache.Cache} to see if a previously transformed resource
 * exists in the cache and returns it if found, and otherwise delegates to the resolver
 * chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
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
      throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
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
