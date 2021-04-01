/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.registry;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.ConcurrentMapCache;
import cn.taketoday.context.EmptyObject;
import cn.taketoday.web.RequestContext;

/**
 * Cache MappedHandlerRegistry
 *
 * @author TODAY 2020/12/11 23:32
 * @since 3.0
 */
public class CacheableMappedHandlerRegistry extends MappedHandlerRegistry {
  static final String CACHE_NAME = CacheableMappedHandlerRegistry.class.getName() + "pattern-matching";

  private Cache patternMatchingCache;

  @Override
  protected Object lookupPatternHandler(final String handlerKey, final RequestContext context) {
    final Cache patternMatchingCache = getPatternMatchingCache();
    Object handler = patternMatchingCache.get(handlerKey, false);
    if (handler == null) {
      handler = lookupCacheValue(handlerKey, context);
      patternMatchingCache.put(handlerKey, handler);
    }
    else if (handler == EmptyObject.INSTANCE) {
      return null;
    }
    return handler;
  }

  protected Object lookupCacheValue(final String handlerKey, final RequestContext context) {
    return super.lookupPatternHandler(handlerKey, context);
  }

  public final Cache getPatternMatchingCache() {
    Cache patternMatchingCache = this.patternMatchingCache;
    if (patternMatchingCache == null) {
      this.patternMatchingCache = patternMatchingCache = createPatternMatchingCache();
    }
    return patternMatchingCache;
  }

  protected ConcurrentMapCache createPatternMatchingCache() {
    return new ConcurrentMapCache(CACHE_NAME, 128);
  }

  /**
   * Set Pattern matching Cache
   *
   * @param patternMatchingCache
   *         a new Cache
   */
  public void setPatternMatchingCache(final Cache patternMatchingCache) {
    this.patternMatchingCache = patternMatchingCache;
  }
}
